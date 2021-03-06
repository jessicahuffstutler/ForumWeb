package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        HashMap<String, User> users = new HashMap();
        ArrayList<Message> messages = new ArrayList();

        addTestUsers(users);
        addTestMessges(messages);

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    //only want to display top level threads
                    ArrayList<Message> threads = new ArrayList();
                    for (Message message : messages) {
                        if (message.replyId == -1) {
                            threads.add(message);
                        }
                    }

                    HashMap m = new HashMap();
                    m.put("threads", threads); //put the thread in the hashmap
                    m.put("username", username); //put the username in the hashmap
                    m.put("replyId", -1);
                    return new ModelAndView(m, "threads.html");
                }),
                new MustacheTemplateEngine()
        );

        Spark.get(
                "/replies",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    HashMap m = new HashMap();
                    m.put("username", username); //telling the replies route to put the username from the session in the HashMap

                    String id = request.queryParams("id");
                    try {
                        int idNum = Integer.valueOf(id);
                        Message message = messages.get(idNum);
                        m.put("message", message);
                        m.put("replyId", message.id); //message.id because we want to reply to that message from above

                        ArrayList<Message> replies = new ArrayList();
                        for (Message msg : messages) {
                            if (msg.replyId == message.id) {
                                replies.add(msg);
                            }
                        }
                        m.put("replies", replies);
                    } catch (Exception e) {

                    }
                    return new ModelAndView(m, "replies.html");
                }),
                new MustacheTemplateEngine()
        );

        Spark.post(
                "/login",
                ((request, response) -> {
                    String username = request.queryParams("username");
                    String password = request.queryParams("password");

                    if (username.isEmpty() || password.isEmpty()) {
                        Spark.halt(403);
                    }

                    User user = users.get(username);
                    if (user == null) {
                        user = new User();
                        user.password = password;
                        users.put(username, user);
                    }
                    else if (password.equals(user.password)) {
                        Spark.halt(403);
                    }

                    Session session = request.session();
                    session.attribute("username", username); //multiple people can be logged in and they will each see their username

                    response.redirect(request.headers("Referer"));
                    return"";
                })
        );

        Spark.post(
                "/create-message",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    if (username == null) {
                        Spark.halt(403);
                    }

                    String replyId = request.queryParams("replyId");
                    String text = request.queryParams("text");
                    try {
                        int replyIdNum = Integer.valueOf(replyId); //getting value of reply id number
                        Message message = new Message(messages.size(), replyIdNum, username, text); //creating message
                        messages.add(message); //adding to arraylist
                    } catch (Exception e) {

                    }

                    response.redirect(request.headers("Referer"));
                    return"";
                })
        );
    }

    static void addTestUsers(HashMap<String, User> users) {
        users.put("Alice", new User());
        users.put("Bob", new User());
        users.put("Charlie", new User());

    }

    static void addTestMessges(ArrayList<Message> messages) {
        messages.add(new Message(0, -1, "Alice", "This is a thread!")); //top level thread (-1)
        messages.add(new Message(1, -1, "Bob", "This is a thread!")); //top level thread (-1)
        messages.add(new Message(2, 0, "Charlie", "Cool thread, Alice.")); //charlie replying to alice (0)
        messages.add(new Message(3, 2, "Alice", "Thanks")); //Alice replying to Charlie (2)
    }
}
