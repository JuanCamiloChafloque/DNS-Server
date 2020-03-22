import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class DNSQuery {

	private int puerto;
	private String dnsExterno;
	private Inet4Address ip;
	private short TID;
	private short flags;
	private short question;
	private short answer;
	private short authority;
	private short aditional;
	private byte[] query;
	private String dom = "";
	private short type;
	private short clase;
	private ArrayList<AnswerRR> records = new ArrayList<AnswerRR>();

	public DNSQuery(byte[] paquete) {

		try {

			DataInputStream din = new DataInputStream(new ByteArrayInputStream(paquete));
			this.TID = din.readShort();
			this.flags = din.readShort();
			this.question = din.readShort();
			this.answer = din.readShort();
			this.authority = din.readShort();
			this.aditional = din.readShort();
			int recLen = 0;
			//Obtener el dominio
			while ((recLen = din.readByte()) > 0) {
				this.query = new byte[recLen];

				for (int i = 0; i < recLen; i++) {
					query[i] += din.readByte();
				}

				dom += new String(query, "UTF-8");

				if(new String(query,"UTF-8").equals("com") || new String(query,"UTF-8").equals("co") || new String(query,"UTF-8").equals("org")) {

				}
				else {
					dom += ".";
				}
			}

			System.out.println("***********Domain: " + dom);
			this.type = din.readShort();
			this.clase = din.readShort();
			//Para servicio externo
			this.puerto = 53;
			this.dnsExterno = "8.8.8.8";
			try {
				this.ip = (Inet4Address) Inet4Address.getByName(this.dnsExterno);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			System.out.println("Error generando el Query.");
		}
	}

	public byte[] realizarConsultaInterna(byte[] paquete, HashMap<String, ArrayList<AnswerRR>> masterF) {

		byte[] respuesta = generarRespuestaInterna(masterF);
		getRecords(respuesta);
		return respuesta;
	}

	public byte[] realizarConsultaExterna(byte[] paquete) {

		try {

			DatagramSocket socket = new DatagramSocket();
			byte[] bytesRespuesta = new byte[1024];

			//Envio a servidor externo
			DatagramPacket paqueteEnvio = new DatagramPacket(paquete, paquete.length, this.ip, this.puerto);
			socket.send(paqueteEnvio);

			//Recibo de servidor externo
			DatagramPacket paqueteRespuesta = new DatagramPacket(bytesRespuesta, bytesRespuesta.length);
			socket.receive(paqueteRespuesta);
			socket.close();
			getRecords(paqueteRespuesta.getData());
			return generarRespuestaExterna();

		} catch (IOException e) {
			System.out.println("Error generando la consulta externa.");
			return null;
		}
	}

	public void getRecords(byte[] respuestaPaq) {

		int respuestas;
		short dom;
		short tip;
		short aClase;
		int ttl;
		short addrLen;
		String Address = "";
		try {

			DataInputStream din = new DataInputStream(new ByteArrayInputStream(respuestaPaq));

			System.out.println("Transaction ID: 0x" + String.format("%x", din.readShort()));
			System.out.println("Flags: 0x" + String.format("%x", din.readShort()));
			System.out.println("Questions: 0x" + String.format("%x", din.readShort()));
			respuestas = din.readShort();
			System.out.println("Cantidad de respuestas: " + respuestas);
			System.out.println("Authority RRs: 0x" + String.format("%x", din.readShort()));
			System.out.println("Additional RRs: 0x" + String.format("%x", din.readShort()));

			int recLen = 0;
			while ((recLen = din.readByte()) > 0) {
				byte[] record = new byte[recLen];

				for (int i = 0; i < recLen; i++) {
					record[i] = din.readByte();
				}
				System.out.println("Record: " + new String(record, "UTF-8"));
			}

			for(int i=0;i<1;i++) {
				din.readShort();
				din.readShort();
			}

			for (int j = 0; j < respuestas; j++) {
				Address = "";

				System.out.println("\n*********** Answer " + (j + 1) + " ***********");
				//Dominio
				dom = din.readShort();
				System.out.println("Name: "+String.format("%x", dom));
				//Tipo
				tip = din.readShort();
				System.out.println("Record Type: 0x" + String.format("%x", tip));
				//Clase
				aClase = din.readShort();
				System.out.println("Class: 0x" + String.format("%x", aClase));
				//TTL
				ttl = din.readInt();
				System.out.println("TTL: 0x" + String.format("%x", ttl));
				//tamano
				addrLen = din.readShort();
				System.out.println("Len: 0x" + String.format("%x", addrLen));

				System.out.print("Address: ");
				for (int i = 0; i < addrLen; i++ ) {
					if(i<3) {
						Address += (din.readByte() & 0xFF) + ".";	
					}else {
						Address += (din.readByte() & 0xFF);
					}
				}
				System.out.println(Address);
				AnswerRR nueva = new AnswerRR(dom, tip, aClase, ttl, addrLen, InetAddress.getByName(Address));
				records.add(nueva);
				System.out.println("***********End of Package***********");
				System.out.println();
				System.out.println();
			}

		} catch (IOException e) {
			System.out.println("Error imprimiendo los records.");
		}
	}

	public byte[] generarRespuestaInterna(HashMap<String, ArrayList<AnswerRR>> masterF) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);
		String[] nombreDom = this.dom.split("\\.");
		try {
			data.writeShort(TID);
			data.writeShort(0x8180);
			data.writeShort(question);
			data.writeShort(masterF.get(this.dom).size());
			data.writeShort(authority);
			data.writeShort(aditional);

			//Formato Query 
			for(int i = 0; i < nombreDom.length; i++) {
				byte[] bytesDom = nombreDom[i].getBytes();
				data.writeByte(bytesDom.length);
				data.write(bytesDom);
			}

			data.writeByte(0x00);
			data.writeShort(0x0001);
			data.writeShort(0x0001);

			//Formato answers
			if(masterF.containsKey(this.dom)) {
				ArrayList<AnswerRR> rec = masterF.get(this.dom);
				for(AnswerRR actual: rec) {
					data.write(actual.toByte());
				}
			}

			return out.toByteArray();

		} catch (IOException e) {
			System.out.println("Error generando la consulta interna.");
			return null;
		}
	}

	public byte[] generarRespuestaExterna() {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);
		String[] nombreDom = this.dom.split("\\.");
		try {
			data.writeShort(TID);
			data.writeShort(0x8180);
			data.writeShort(question);
			data.writeShort(records.size());
			data.writeShort(authority);
			data.writeShort(aditional);

			//Formato Query 
			for(int i = 0; i < nombreDom.length; i++) {
				byte[] bytesDom = nombreDom[i].getBytes();
				data.writeByte(bytesDom.length);
				data.write(bytesDom);
			}

			data.writeByte(0x00);
			data.writeShort(0x0001);
			data.writeShort(0x0001);

			//Formato respuestas
			for (AnswerRR r: records) {
				data.write(r.toByte());
			}
			return out.toByteArray();

		} catch (IOException e) {
			System.out.println("Error generando el paquete externo.");
			return null;
		}
	}

	public int getPuerto() {
		return puerto;
	}

	public void setPuerto(int puerto) {
		this.puerto = puerto;
	}

	public String getDnsExterno() {
		return dnsExterno;
	}

	public void setDnsExterno(String dnsExterno) {
		this.dnsExterno = dnsExterno;
	}

	public Inet4Address getIp() {
		return ip;
	}

	public void setIp(Inet4Address ip) {
		this.ip = ip;
	}

	public short getTID() {
		return TID;
	}

	public void setTID(short tID) {
		TID = tID;
	}

	public short getFlags() {
		return flags;
	}

	public void setFlags(short flags) {
		this.flags = flags;
	}

	public short getQuestion() {
		return question;
	}

	public void setQuestion(short question) {
		this.question = question;
	}

	public short getAnswer() {
		return answer;
	}

	public void setAnswer(short answer) {
		this.answer = answer;
	}

	public short getAuthority() {
		return authority;
	}

	public void setAuthority(short authority) {
		this.authority = authority;
	}

	public short getAditional() {
		return aditional;
	}

	public void setAditional(short aditional) {
		this.aditional = aditional;
	}

	public byte[] getQuery() {
		return query;
	}

	public void setQuery(byte[] query) {
		this.query = query;
	}

	public String getDominio() {
		return this.dom;
	}

	public void setDom(String dom) {
		this.dom = dom;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public short getClase() {
		return clase;
	}

	public void setClase(short clase) {
		this.clase = clase;
	}

	public ArrayList<AnswerRR> getRecords() {
		return records;
	}

	public void setRecords(ArrayList<AnswerRR> records) {
		this.records = records;
	}
}
