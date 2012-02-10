
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1){
			System.out.println("You have to at least provide a port number");
			return;
		}
		String path = args.length < 2 ? null : args[1];
		new Server(new Integer(args[0]), path);
	}

}
