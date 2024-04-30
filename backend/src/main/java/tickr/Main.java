package tickr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tickr.application.apis.ApiLocator;
import tickr.application.apis.email.IEmailAPI;
import tickr.application.apis.email.NullEmailAPI;
import tickr.application.apis.email.SendGridAPI;
import tickr.application.apis.location.ILocationAPI;
import tickr.application.apis.location.NominatimAPI;
import tickr.application.apis.purchase.IPurchaseAPI;
import tickr.application.apis.purchase.NullPurchaseAPI;
import tickr.application.apis.purchase.StripeAPI;
import tickr.persistence.HibernateModel;
import tickr.server.Server;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    private static void printUsage () {
        System.out.println("Usage: ");
        System.out.println("./server.sh [<port>] [<options>]");
        System.out.println("Accepted options: ");

        System.out.println("  --help\n\tPrints this help message.\n");
        System.out.println("  -live-email\n\tUses live SendGrid email API\n");
        System.out.println("  -live-stripe-secret=<secret>\n\t Uses live Stripe API. The passed in secret is the endpoint secret obtained from stripe listen");
    }
    public static void main(String[] args) {
        var databaseModel = new HibernateModel();

        int port = 8080;

        boolean useLiveEmail = false;
        boolean useLivePurchase = false;
        String stripeSecret = null;

        int inNum = 0;

        for (var arg : args) {
            if (!arg.trim().startsWith("-")) {
                if (inNum != 0) {
                    System.err.println("Unexpected argument: \"" + arg + "\"");
                    printUsage();
                    System.exit(1);
                }

                port = Integer.parseInt(arg);
                inNum++;
            } else if (arg.trim().startsWith("-live-stripe-secret")) {
                var split = arg.split("=");
                if (split.length == 1) {
                    System.err.println("Option -live-stripe-secret requires a secret to be passed in!");
                    printUsage();
                    System.exit(1);
                }
                stripeSecret = split[1].trim();
                useLivePurchase = true;
            } else if (arg.trim().equals("-live-email")) {
                useLiveEmail = true;
            } else if (arg.trim().equals("--help")) {
                printUsage();
                System.exit(0);
            } else {
                System.err.println("Unknown option: \"" + arg.trim() + "\"");
                printUsage();
                System.exit(1);
            }
        }
        if (useLiveEmail) {
            logger.info("Using live SendGrid email API!");
            ApiLocator.addLocator(IEmailAPI.class, SendGridAPI::new);
        } else {
            logger.info("Using testing email API!");
            ApiLocator.addLocator(IEmailAPI.class, NullEmailAPI::new);
        }

        int portFinal = port;
        if (useLivePurchase) {
            logger.info("Using live Stripe payments API!");
            String finalStripeSecret = stripeSecret;
            ApiLocator.addLocator(IPurchaseAPI.class, () -> new StripeAPI(finalStripeSecret));
        } else {
            logger.info("Using testing payments API!");
            ApiLocator.addLocator(IPurchaseAPI.class, () -> new NullPurchaseAPI("http://localhost:" + portFinal));
        }
        logger.info("Using live Nominatim geocoding API!");
        ApiLocator.addLocator(ILocationAPI.class, () -> new NominatimAPI(databaseModel));

        logger.info("Starting tickr server on http://localhost:{}!", port);

        Server.start(port, null, databaseModel);
    }
}