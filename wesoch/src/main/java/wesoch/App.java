package wesoch;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import wesoch.websocket.ChatEndpoint;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

public class App {
    public static int CONFIG_PORT = 7001;

    public static void main(String[] args) {
        Server httpServer = new Server();
        ServerConnector httpConnector = new ServerConnector(httpServer);

        httpConnector.setPort(App.CONFIG_PORT);
        httpConnector.setHost("0.0.0.0");

        httpServer.addConnector(httpConnector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");
        httpServer.setHandler(context);

        ServerContainer wsServerContainer = null;

        try {
            // Initialize javax.websocket layer
            wsServerContainer = WebSocketServerContainerInitializer.configureContext(context);
        } catch (ServletException e) {
            System.out.println("[ERROR] Could not initialize the javax.websocket layer (Exception message: " + e.getMessage() + ")");
            System.exit(1);
        }

        try {
            // Add WebSocket endpoint to javax.websocket layer
            wsServerContainer.addEndpoint(ChatEndpoint.class);
        } catch (DeploymentException e) {
            System.out.println("[ERROR] Could not add the ChatEndpoint to the ServerContainer (Exception message: " + e.getMessage() + ")");
            System.exit(1);
        }

        try {
            httpServer.start();
            //httpServer.dump(System.err);
            httpServer.join();
        } catch (Throwable t) {
            System.out.println("[ERROR] Could not start the Server (Throwable message: " + t.getMessage() + ")");
            System.exit(1);
        }
    }
}
