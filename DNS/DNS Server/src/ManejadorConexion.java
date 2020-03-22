import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class ManejadorConexion extends Thread{
	private DatagramPacket peticion;
	private int puerto;
	private InetAddress cliente;
	private DatagramSocket udp;
	private HashMap<String, ArrayList<AnswerRR>> masterF;

	public ManejadorConexion(DatagramPacket peticion,int puerto, InetAddress cliente, DatagramSocket udp, HashMap<String, ArrayList<AnswerRR>> masterF) {
		this.peticion = peticion;
		this.puerto =puerto;
		this.cliente = cliente;
		this.udp = udp;
		this.masterF = masterF;
	}

	public void run() {

		byte[] respuesta = new byte[1024];
		DNSQuery query = new DNSQuery(peticion.getData());

		if(this.masterF.containsKey(query.getDominio())) {
			System.out.println("Se encontró el dominio en el MasterFile Interno");
			respuesta = query.realizarConsultaInterna(peticion.getData(), this.masterF);
		}
		else {
			System.out.println("Se encontró el dominio en el MasterFile Externo");
			respuesta = query.realizarConsultaExterna(peticion.getData());
		}

		DatagramPacket paquete = new DatagramPacket(respuesta,respuesta.length, this.cliente,this.puerto);
		try {
			udp.send(paquete);
			this.udp.close();
		} catch (Exception e) {
			System.out.println("Enviando...");
		}
	}
}
