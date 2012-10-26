import static spark.Spark.*;
import spark.*;
import java.io.IOException;
import java.util.HashMap;
import com.twocheckout.*;
import com.twocheckout.model.Sale;

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
                //Create a form that automatically passes the buyer and sale to 2Checkout
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("sid", "1817037");
                params.put("mode", "2CO");
                params.put("li_0_name", "Awesome Product");
                params.put("li_0_price", "1.00");
                String form = TwocheckoutCharge.submit(params);
                form = "<html>" + form + "</html>";
                return form;
            }
        });
        
        get(new Route("/return") {
            @Override
            public Object handle(Request request, Response response) {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("sid", request.queryParams("sid"));
                params.put("total", request.queryParams("total"));
                params.put("order_number", request.queryParams("order_number"));
                params.put("key", request.queryParams("key"));
                Boolean result = TwocheckoutReturn.check(params, "tango");
                if (result) {
                    //Email product to the buyer
                    SendMail.send(request.queryParams("email"));
                    //Add comment to the order for our records.
                    //(Not Required: Just here to demonstrate API usage.)
                    Twocheckout.apiusername = "APIuser1817037";
                    Twocheckout.apipassword = "APIpass1817037";
                    Sale sale;
					try {
						sale = TwocheckoutSale.retrieve(request.queryParams("order_number"));
                    HashMap<String, String> comment_params = new HashMap<String, String>();
                    comment_params.put("sale_comment", "Product delivered to buyer by email.");
						sale.comment(comment_params);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

                    //Display Order Success Page
                    FileRead file = new FileRead();
                    String content = "Please try again later.";
                    try {
                        content = file.readFile("static/return.html");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return content;
                } else {
                    return "There was a problem with your order, please contact us for assistance.";
                }
            }
        });
        
        post(new Route("/notification") {
            @Override
            public Object handle(Request request, Response response) {
                String output = "No Message Handled.";
                //Handle an INS message, in this case we will listen for Fraud Status Changed
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("vendor_id", request.queryParams("vendor_id"));
                params.put("invoice_id", request.queryParams("invoice_id"));
                params.put("sale_id", request.queryParams("sale_id"));
                params.put("md5_hash", request.queryParams("md5_hash"));
                Boolean result = TwocheckoutNotification.check(params, "tango");
                if (result) {
                    if (request.queryParams("message_type").equals("FRAUD_STATUS_CHANGED") && request.queryParams("fraud_status").equals("pass")) {
                        //Here you can deliver the product or do whatever you want
                        SendMail.send(request.queryParams("customer_email"));
                        output = "Success";
                    } else {
                        //Here you can contact the buyer or do whatever you want
                        output = "Fail";
                    }
                }
                return output;
            }
        });
    }

}

