## 2Checkout Java Integration Tutorial
----------------------------------------

In this tutorial we will walk through integrating the 2Checkout payment method into an existing site built on the Spark micro framework and TwitterBootstrap 2.1.0. The source for the example application used in this tutorial can be accessed in this Github repository.

Setting up the Example Application
----------------------------------

We need an existing example application to demonstrate the integration so lets download or clone the 2checkout-java-tutorial tutorial application.

```shell
$ git clone https://github.com/2checkout/2checkout-java-tutorial.git
```

This repository contains both an example before and after application so that we can follow along with the tutorial using the site\_before app and compare the result with the site\_after app. We can start by moving the site\_before directory to the webroot directory on our server.

We will need to pull in some dependencies to get the application working.

* [Spark -v 0.9.9.3 Snapshot and dependenciess](http://code.google.com/p/spark-java/downloads/list)
* [JavaMail -v 1.4.5](http://www.oracle.com/technetwork/java/javamail/index-138643.html)


Let's go ahead and startup the example application by running it in our IDE.

We can then view it at [http://localhost:4567](http://localhost:4567).

![](http://github.com/2checkout/2checkout-java-tutorial/raw/master/img/egood-1.png)

You can see that this site requires users to enter in their email address so we can email them the downloadable product.

The SendMail class is configured to use gmail so to see a working demonstration, just add your gmail address for the username and add your gmail password.

In this tutorial, we will integrate the 2Checkout payment method so that the user must purchase the product before we send it by email. We will also setup a listener that can be used to send the product out on the Fraud Status Changed notification send by 2Checkout. To provide an example API usage, we will fire a create comment API call to add a note to the order when we have delivered the product to the buyer.


Adding the 2Checkout Java Library
------------------------------------
The 2Checkout Java library provides us with a simple bindings to the API, INS and Checkout process so that we can integrate each feature with only a few lines of code. We can download or clone the library at [https://github.com/2checkout/2checkout-java](https://github.com/2checkout/2checkout-java).

Including the library is as easy as [downloading the jar](https://github.com/downloads/2checkout/2checkout-java/twocheckout-java-latest.jar), adding it and it's dependencies to your project, and importing into Home.java.

```java
import com.twocheckout.*;
import com.twocheckout.model.Sale;
```

Setup Order
-----------
Let's take a look at our current application.


```java
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
```

As you can see, we are just grabbing the email address entered by the user and sending them the downloadable product by email. We want the user to have to pay for the product before we actually deliver it to them so lets go ahead and replace that form with a link to the order method and pass the buyer to 2Checkout.

**Home.html**
```html
    <div class="row">
        <div class="span12">
            <p><a class="btn btn-primary btn-large" href="/order">Buy Now</a></p>
            <h2>Only $1.00!</h2>
            <p>For one low price you get access to this great downloadable product!</p>
        </div>
    </div>
```


**Home.java**
```java
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
```

Lets take a look at what we did here. First swapped the email form with a link to the order page because we no longer need to collect the users email. (_It will be collected and returned by 2Checkout._)

```html
<a class="btn btn-primary btn-large" href="/order">Buy Now</a>
```

Then we create a HashMap of sale parameters to pass to 2Checkout using the [Pass-Through-Products parameter set](https://www.2checkout.com/blog/knowledge-base/merchants/tech-support/3rd-party-carts/parameter-sets/pass-through-product-parameter-set/).

```java
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("sid", "1817037");
    params.put("mode", "2CO");
    params.put("li_0_name", "Awesome Product");
    params.put("li_0_price", "1.00");
```

Finially we pass the parameters and the buyer to our custom checkout page on 2Checkout's secure server to complete the order.

```java
    String form = TwocheckoutCharge.submit(params);
    form = "<html>" + form + "</html>";
    return form;
```

Passback
--------

When the order is completed, 2Checkout will return the buyer and the sale parameters to the URL that we specify as the approved URL in our account. This URL can also be passed in dynamically for each sale using the `x_receipt_link_url` parameter.

Lets create the return URL by adding a return method. We will also need to setup our return method to handle the passback and we will utilize 2Checkout's API to add a comment to the sale when we deliver the product to the buyer.

Home.java
```java
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
                Sale sale = TwocheckoutSale.retrieve(request.queryParams("order_number"));
                HashMap<String, String> comment_params = new HashMap<String, String>();
                comment_params.put("sale_comment", "Product delivered to buyer by email.");
                sale.comment(comment_params);

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
```

First we need to validate the MD5 Hash returned by 2Checkout, so we grab the `sid`, `total`, `order_number` and `key` from the parameters returned by 2Checkout and assign them to the `params` HashMap.

```java
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("sid", request.queryParams("sid"));
    params.put("total", request.queryParams("total"));
    params.put("order_number", request.queryParams("order_number"));
    params.put("key", request.queryParams("key"));
```

Then we pass the HashMap and our secret word to the `TwocheckoutReturn.check()` binding as the first argument and our secret word as the second argument. This method will return true if the hash matches.

```java
    Boolean result = TwocheckoutReturn.check(params, "tango");
```

If the result true, we send the email to the buyer, add a comment to the sale using 2Checkout's API and display the return page. If the response is false, we display and error message.

```java
    if (result) {
        //Email product to the buyer
        SendMail.send(request.queryParams("email"));
        //Add comment to the order for our records.
        //(Not Required: Just here to demonstrate API usage.)
        Twocheckout.apiusername = "APIuser1817037";
        Twocheckout.apipassword = "APIpass1817037";
        Sale sale = TwocheckoutSale.retrieve(request.queryParams("order_number"));
        HashMap<String, String> comment_params = new HashMap<String, String>();
        comment_params.put("sale_comment", "Product delivered to buyer by email.");
        sale.comment(comment_params);

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
```

Now we can setup our return method, enter our secret word and provide the approved URL path "http://localhost:4567/return" under the Site Management page in our 2Checkout admin.

**Site Management Page**
![](http://github.com/2checkout/2checkout-java-tutorial/raw/master/img/egood-2.png)

**Lets try it out with a live sale.**
![](http://github.com/2checkout/2checkout-java-tutorial/raw/master/img/egood-1.png)

**Enter in our billing information and submit the payment.**
![](http://github.com/2checkout/2checkout-java-tutorial/raw/master/img/egood-4.png)

![](http://github.com/2checkout/2checkout-java-tutorial/raw/master/img/egood-5.png)

Notifications
-------------

2Checkout will send notifications to our application under the following circumstances.

* Order Created
* Fraud Status Changed
* Shipping Status Changed
* Invoice Status Changed
* Refund Issued
* Recurring Installment Success
* Recurring Installment Failed
* Recurring Stopped
* Recurring Complete
* Recurring Restarted

For our application, we are interested in the Fraud Status Changed message in case we would rather wait for the 2Checkout fraud review result before sending the file to the buyer. To handle this message, we will create a new notification route.

Home.java
```java
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

```

We grab the message parameters and assign the `vendor_id`, `invoice_id`, `sale_id` and `md5_hash` values to a new params HashMap.

```java
    HashMap<String, String> params = new HashMap<String, String>();
    params.put("vendor_id", request.queryParams("vendor_id"));
    params.put("invoice_id", request.queryParams("invoice_id"));
    params.put("sale_id", request.queryParams("sale_id"));
    params.put("md5_hash", request.queryParams("md5_hash"));
```

Then we pass the HashMap and our secret word to the `Notification.check()` binding as the first argument and our secret word as the second argument. This method will return true if the hash matches.

```java
    Boolean result = TwocheckoutNotification.check(params, "tango");
```

If the response is true, we can preform actions based on the `message_type` parameter value.

For the Fraud Status Changed message, we will also check the value of the `fraud_status` parameter and only send the product if it equals 'pass'.

```java
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
```

Now we can setup our Notification URL path "http://localhost:4567/notification" and enable the Fraud Status Changed message under the Notifications page in our 2Checkout admin.

![](http://github.com/2checkout/2checkout-java-tutorial/raw/master/img/egood-6.png)

Lets test our notification function. Now there are a couple ways to go about this. You can wait for the notifications to come on a live sale, or just head over to the [INS testing tool](http://developers.2checkout.com/inss) and test the messages right now. Remember the MD5 hash must match so for easy testing, you must compute the hash based on like below:

`UPPERCASE(MD5_ENCRYPTED(sale\_id + vendor\_id + invoice\_id + Secret Word))`

You can just use an [online MD5 Hash generatorr](https://www.google.com/webhp?q=md5+generator) and convert it to uppercase.

Conclusion
----------

Now our application is fully integrated! Our buyers can pay for the product and we wither email it to them immediately or wait for fraud review.
