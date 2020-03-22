import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;


public class AnswerRR {

	private short name;
	private short tipo;
	private short clase;
	private int TLL;
	private short length;
	private InetAddress address;

	public AnswerRR(short name, short tipo, short clase, int tLL, short length, InetAddress address) {

		super();
		this.name = name;
		this.tipo = tipo;
		this.clase = clase;
		this.TLL = tLL;
		this.length = length;
		this.address = address;
	}

	public byte[] toByte() {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream data = new DataOutputStream(out);

		try {
			data.writeShort(name);
			data.writeShort(tipo);
			data.writeShort(clase);
			data.writeInt(TLL);
			data.writeShort(length);
			data.write(address.getAddress());

			return out.toByteArray();
		} catch (IOException e) {
			System.out.println("Error parsendo la respuesta del paquete.");
			return null;
		}
	}

	public short getName() {
		return name;
	}

	public void setName(short name) {
		this.name = name;
	}

	public short getTipo() {
		return tipo;
	}

	public void setTipo(short tipo) {
		this.tipo = tipo;
	}

	public short getClase() {
		return clase;
	}

	public void setClase(short clase) {
		this.clase = clase;
	}

	public int getTLL() {
		return TLL;
	}

	public void setTLL(int tLL) {
		this.TLL = tLL;
	}

	public short getLength() {
		return length;
	}

	public void setLength(short length) {
		this.length = length;
	}

	public InetAddress getAdress() {
		return address;
	}

	public void setAdress(InetAddress adress) {
		this.address = adress;
	}
}
