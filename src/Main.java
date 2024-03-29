
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2){
			System.out.println("Missing parameters");
			System.out.println("Usage: java Main <port> <server name>");
			return;
		}
		String path = args.length < 3 ? null : args[2];
		new Server(new Integer(args[0]), args[1], path);
		System.exit(0);
	}

}
