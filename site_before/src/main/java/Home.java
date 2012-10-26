import static spark.Spark.*;
import spark.*;
import java.io.IOException;

public class Home {

    public static void main(String[] args) {

        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                //Display Homepage
                FileRead file = new FileRead();
                String content = "Please try again later.";
                try {
                    content = file.readFile("static/home.html");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return content;
            }
        });

        get(new Route("/order") {
            @Override
            public Object handle(Request request, Response response) {
                SendMail.send(request.queryParams("email"));
                return "Your download was sent by email.";
            }
        });
    }

}

