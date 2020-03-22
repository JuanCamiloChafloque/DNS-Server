import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class Servidor {
	
	private int port;
	private int udpSize;
	private HashMap<String, ArrayList<AnswerRR>> masterFile;
	
	public Servidor(){
	
		this.port = 53;
		this.udpSize = 512;
		this.masterFile = new HashMap<String, ArrayList<AnswerRR>>();
		try {
			llenarMasterFile();
		} catch (Exception e) {
			System.out.println("Error creando el masterfile interno");
		}
	}
	
	public void llenarMasterFile() throws Exception {

		BufferedReader br = new BufferedReader(new FileReader("src\\MasterFile.txt"));
		String linea; 
		String dominio = "";
		InetAddress ip;
		byte[] name;
		int ttl;
		short tipo;
		short clase;
		short len = 4;

		while((linea = br.readLine()) != null ) {
			
			String[] datos = linea.split(" ");
			
			if(!datos[0].equalsIgnoreCase("$ORIGIN")) {
				
				ArrayList<AnswerRR> ips = new ArrayList<AnswerRR>();

				dominio = datos[0];
				name = datos[0].getBytes();
				ttl = Integer.parseInt(datos[1]);
				tipo = 0x0001;
				clase = 0x0001;
				ip = InetAddress.getByName(datos[4]);
				AnswerRR resp = new AnswerRR(convertToShort(name), tipo, clase, ttl, len, ip);
				
				if(this.masterFile.containsKey(dominio)) {
					this.masterFile.get(dominio).add(resp);
				}
				else {
					ips.add(resp);
					this.masterFile.put(dominio, ips);
				}
			}
		}

		br.close();

	}
	
	public short convertToShort(byte[] name) {
		
		ByteBuffer buffer = ByteBuffer.wrap(name);
		return buffer.getShort();
	}
	
	public void enviarPeticionCliente() throws Exception {
		
		byte[] bufer = new byte[this.udpSize];
		
		while(true) {
			DatagramSocket socketUDP = new DatagramSocket(this.port);
			DatagramPacket peticion = new DatagramPacket(bufer, bufer.length);
			socketUDP.receive(peticion);
			socketUDP.setSoTimeout(10000);
	        System.out.print("Datagrama recibido del host: " + peticion.getAddress());
	        System.out.println(" desde el puerto remoto: " + peticion.getPort());
	        ManejadorConexion manejador = new ManejadorConexion(peticion, this.port, peticion.getAddress(), socketUDP, this.masterFile);
	        manejador.start();
		}
	}
	
	public static void main(String[] args) {
		
		Servidor servidorDNS = new Servidor();
		try {
			servidorDNS.enviarPeticionCliente();
		} catch (Exception e) {
			System.out.println("Timeout reached...");
		}
	}
}
