import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;


public class Server {
    static int portNumber = 1948;
    static int missingPacket = -1;
    static int totalPackets = 0;
    static Random r = new Random();

    public static void main(String[] args) {


        try(
                ServerSocket server = new ServerSocket(portNumber); //create new server socket
                Socket socket = server.accept(); //accept reqs on the socket
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true); //make printwriter to write to output stream
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //make buffered reader to get input stream
                ){
            String usersRequest;

            while ((usersRequest = bufferedReader.readLine()) != null) { //when there is line in the buffer, ie we recieved a req

                Requests req = parseReq(usersRequest); // parse req -- see if it is a :GET: req or a missing packet :GET:{number} req

                File file = new File("/Users/aharon/Desktop/VSTP/out/production/VSTP/Gettysburg.txt"); //get file to send
                String data = Files.readString(file.toPath());

                if (req == Requests.GET){

                    String[] packetsToSend = new String[21];

                    for (int i=0; i < data.length(); i += data.length() / 20, totalPackets++){  //divide the total length by 20, so 21 full packets
                        if (r.nextInt(10) >= 8){
                            continue;
                        }
                        if (data.length() < i + data.length() / 20) {
                            packetsToSend[totalPackets] = totalPackets + ":DATA:" + data.substring(i);
                        } else {
                            packetsToSend[totalPackets] = totalPackets + ":DATA:" + data.substring(i, i + data.length()/20 );
                        }
                    }

                    Collections.shuffle(Arrays.asList(packetsToSend)); //shuffle all the packets

                    for (String packet : packetsToSend) { //send packets
                        if (packet == null) continue; // if dropped packed do not send

                        printWriter.println(packet);

                        System.out.println("Sending packet: " + packet.split(":")[0]);
                    }

                    printWriter.println(totalPackets + ":DONE:" + totalPackets); //end confirmation

                } else if (req == Requests.MISSING_PACKET){
                    if (r.nextInt(10) < 8) { //drop the packet 20% of the time

                        if (data.length() < (missingPacket * data.length() / 20)  + data.length() / 20) {
                        printWriter.println(missingPacket + ":DATA:" + data.substring((missingPacket * data.length() / 20)));

                    }
                    } else {
                        if (r.nextInt(10) < 8) { //drop the packet 20% of the time
                            System.out.println("sending:");
                            System.out.println(missingPacket + ":DATA:" + data.substring((missingPacket * data.length() / 20), (missingPacket * data.length() / 20) + data.length() / 20));

                            printWriter.println(missingPacket + ":DATA:" + data.substring((missingPacket * data.length() / 20), (missingPacket * data.length() / 20) + data.length() / 20));
                        }
                    }
                    printWriter.println(totalPackets + ":DONE:" + totalPackets); //end confirmation
                }
            }

    } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Requests parseReq(String usersRequest) {
        String [] reqParts = usersRequest.split(":", -1); // req format is ":GET:number?" so we will split on the colons to get the req type and the optional number (optional number signifiys missing packed req) [-1 is so don't remove trailing space-> ":GET:" = ["", "GET", ""]


        if (reqParts[1].equals("GET") && reqParts[2].isEmpty()){ // because 3rd part is empty it is not missing packet req
            System.out.println("Recived GET req");
            return Requests.GET;
        } else if (reqParts[1].equals("GET") && !reqParts[2].isEmpty()) { //if not empty
            try {
                 missingPacket = Integer.valueOf(reqParts[2]);
                 return Requests.MISSING_PACKET;
            } catch (NumberFormatException e){

            }

        }
        return null;
    }
}