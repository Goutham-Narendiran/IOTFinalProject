import org.eclipse.paho.client.mqttv3.*;

import java.sql.*;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class DeviceSim  {

    //Initialize variables and values
    public String output = "STATUS: ";
    Random rand = new Random();


    //initialize the Lat and Long for each device in case database fails
    double d1lat = 43.95;
    double d1lng = -78.90;
    double d2lat = 43.8926535;
    double d2lng = -78.940705;
    double d3lat = 43.876243;
    double d3lng = -79.043361;
    double d4lat = 43.844288;
    double d4lng =-79.104954;
    double [] dLat = {d1lat, d2lat, d3lat, d4lat};
    double [] dLng = {d1lng, d2lng, d3lng, d4lng};

    public static void main(String[] args) throws Exception {

        //HiveMQ broker for MQTT
        String broker = "tcp://broker.hivemq.com:1883";
        DeviceSim ds = new DeviceSim(broker);
    }

    public DeviceSim(String broker) throws Exception {
        //establish MQTT connection
        final String publisherId = UUID.randomUUID().toString();
        final IMqttClient mqttClient = new MqttClient(broker, publisherId);
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        mqttClient.connect(options);



        //Start each device on a thread
        new DeviceThread(mqttClient, "device1", 0).start();
        new DeviceThread(mqttClient, "device2", 1).start();
        new DeviceThread(mqttClient, "device3", 2).start();
        new DeviceThread(mqttClient, "device4", 3).start();


    }

    //thread for simulted device
    class DeviceThread extends Thread{
        //initialize variables
        protected IMqttClient mqttClient;
        int device;
        String topic;
        String devName;
        public DeviceThread(IMqttClient client, String devicename, int deviceid){
            this.mqttClient = client;
            this.device = deviceid;
            this.topic = "deviceloc/latlong/" + devicename;
            this.devName = devicename;
    }



        public void run(){
            //establish connection to sql database
            String url = "jdbc:mysql://localhost:3306/devicemgmt";
            String user = "root";
            String pass = "root1234";
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(url, user, pass);
                System.out.println("Connected to database.");
                Statement statement = conn.createStatement();
                //update database with latest coordinates
                //from database using SQL query
                String query = "SELECT * FROM " + devName;
                ResultSet rs = statement.executeQuery(query);

                while (rs.next()){
                    dLat[device]= rs.getDouble("lat");
                    dLng[device]= rs.getDouble("lng");

                    // print the results to console
                    System.out.println(devName + ": Latitude: " +  dLat[device] + " Longtitude: " + dLng[device]);
                }

                //Initialize counter
                //used to keep track of run time locally
                int counter = 0;
                //forever loop
                while (true) {

                    String latlong = "";

                    //move Lat by a small random variation
                    dLat[device] = dLat[device]+randGen();
                    latlong += dLat[device]  + "&"; //save Lat to output variable for MQTT

                    //move Long by a small random variation
                    dLng[device] = dLng[device]+randGen();
                    latlong +=  dLng[device];//save to output for mqtt

                    //prep msg to publish
                    MqttMessage message = new MqttMessage();
                    message.setPayload((latlong).getBytes()); //convert to byte
                    message.setQos(1);//set level

                    //publish msg to corresponding topic
                    mqttClient.publish(topic, message);

                    //print to console
                    System.out.println(devName + ": Latitude: " +  dLat[device] + " Longtitude: " + dLng[device]);

                    //save last know location into database every 60 seconds
                    if(counter == 30){ //counter set to 30, because loop sleeps for 2 secs every run
                        System.out.println("Recording location for " + devName);
                        query = "INSERT INTO "+ devName+" (lat, lng) VALUES ("+dLat[device]+","+ dLng[device]+");";
                        //Statement statement = conn.createStatement();
                        statement.execute(query);
                        counter = 0;//reset counter after saving to database
                    }

                    TimeUnit.SECONDS.sleep(2);//wait 2 seconds
                    counter++;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } catch (MqttException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //random num generator
    public double randGen (){
        //random number from 1 to 9
        double random =  rand.nextInt(10);
        random = random/1000; //divide by 1000 to make smaller increment
        return random;
    }


}