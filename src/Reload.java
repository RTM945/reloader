import java.lang.instrument.Instrumentation;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public enum Reload implements Remote {

    INSTANCE;

    private static final String NAME = "reload";
    private static final int PORT = 9999;

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        Reload reload = getInstance();
        reload.setInst(inst);
        Registry registry = LocateRegistry.createRegistry(PORT);
        Remote stub = UnicastRemoteObject.exportObject(reload, PORT);
        registry.rebind(NAME, stub);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        premain(agentArgs, inst);
    }

    public static void main(String[] args) throws Exception {
        String rmiHost = "127.0.0.1";
        int rmiPost = 9999;
        String jar = "hotdeploy.jar";
        for (int i = 0; i < args.length; i ++) {
            String arg = args[i];
            if ("-rmiHost".equalsIgnoreCase(arg)) {
                i++;
                rmiHost = args[i];
            } else if ("-rmiPort".equalsIgnoreCase(arg)) {
                i++;
                rmiPost = Integer.parseInt(args[i]);
            } else if ("-deploy".equalsIgnoreCase(arg)) {
                i++;
                jar = args[i];
            }
        }
        Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPost);
        Reload reload = (Reload) registry.lookup(NAME);
        reload.reload(jar);
    }


    private Instrumentation inst;

    public static Reload getInstance() {
        return INSTANCE;
    }


    public void reload(String jar) {
        HotDeployJar hotDeployJar = new HotDeployJar(jar, inst);
        hotDeployJar.exec();
    }

    public void setInst(Instrumentation inst) {
        this.inst = inst;
    }

}
