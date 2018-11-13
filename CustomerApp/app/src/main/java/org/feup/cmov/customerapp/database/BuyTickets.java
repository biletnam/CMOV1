package org.feup.cmov.customerapp.database;

import android.util.Log;

import org.feup.cmov.customerapp.dataStructures.Ticket;
import org.feup.cmov.customerapp.dataStructures.User;
import org.feup.cmov.customerapp.shows.ShowActivity;
import org.feup.cmov.customerapp.utils.Constants;
import org.feup.cmov.customerapp.utils.MyCrypto;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BuyTickets extends ServerConnection implements Runnable {

    // shows' activity
    private ShowActivity activity;

    // id of show to get tickets from
    private int showID;

    // id of user buy tickets
    private User user;

    // quantity of tickets to buy
    private int quantity;

    public BuyTickets(ShowActivity activity, int showID, User user, int quantity) {
        this.activity = activity;
        this.showID = showID;
        this.user = user;
        this.quantity = quantity;
    }

    @Override
    public void run() {
        URL url;
        HttpURLConnection urlConnection = null;

        int responseCode = Constants.NO_INTERNET;

        try {
            url = new URL("http://" + address + ":" + port + "/shows/" + showID + "/tickets");
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(Constants.SERVER_TIMEOUT);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setUseCaches(false);

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("userId", this.user.getId());
            jsonParam.put("quantity", this.quantity);

            String signedMessage = MyCrypto.signMessage(this.user.getUsername(), jsonParam);

            urlConnection.connect();

            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(signedMessage);
            out.close();

            responseCode = urlConnection.getResponseCode();
            String response;

            if (responseCode == Constants.OK_RESPONSE) {
                response = readStream(urlConnection.getInputStream());

                // get new shows from server
                List<Ticket> tickets = jsonToArray(response);

                // notifies activity that loading finished
                //activity.handleResponse(responseCode, response, tickets);

            } else {
                response = readStream(urlConnection.getErrorStream());
                //activity.handleResponse(responseCode, response, null);
            }
        } catch (Exception e) {
            if (responseCode == Constants.NO_INTERNET) {
                String errorMessage = Constants.ERROR_CONNECTING;
                //activity.handleResponse(responseCode, errorMessage, null);
            }
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }

    }

    private List<Ticket> jsonToArray(String jsonString) {
        List<Ticket> tickets_list = new ArrayList<>();

        try {
            JSONArray jArray = new JSONArray(jsonString);

            for(int i=0; i<jArray.length(); i++){
                JSONObject ticket = jArray.getJSONObject(i);

                int id = ticket.getInt("id");
                String name = ticket.getString("name");
                String date = ticket.getString("date");
                int seatNumber = ticket.getInt("seatNumber");
                double price = ticket.getDouble("price");

                Ticket t = new Ticket(id, name, date, seatNumber, price);
                tickets_list.add(t);
            }
        } catch(JSONException e) {
            Log.e("ERROR", e.getMessage());
        }

        return tickets_list;
    }
}