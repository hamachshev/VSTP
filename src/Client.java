import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Client {

    static ArrayList<String> responses = new ArrayList<>();
    public static void main(String[] args) {
        int portNumber = 1948;
        try(
                Socket client = new Socket("127.0.0.1", 1948); //create new client socket
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true); //make printwriter to write to output stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream())); //make buffered reader to get input stream
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in)); // make buffered reader for users input
                ) {
            String userInput;
            String serverResponse;
            while ((userInput = stdIn.readLine()) != null) { // get input
                writer.println(userInput);
                while ((serverResponse = reader.readLine()) != null) { // wait for server response
                    int missingPacket = handleResponse(serverResponse); // handle response and check for missing packet
                    if ( missingPacket > -1){
                        System.out.println("Sending missing packet req for packet: " + missingPacket);
                        writer.println(":GET:" + missingPacket); // missing packet request
                    }
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int handleResponse(String serverResponse) {
        String [] responseSplit = serverResponse.split(":"); // split the response on the colons
        if (responseSplit[1].equals("DONE")){ // this is the last packet
            int missingPacket = verifyResponse(responseSplit); // check for missing packet
            if (missingPacket == -1){ // ie no missing packet
                System.out.println(responses.stream() //print out response
                        .sorted((res1, res2) ->
                            Integer.parseInt(res1.split(":")[0]) > Integer.parseInt(res2.split(":")[0]) ? 1 : -1
                        )
                        .map(res -> res.split(":")[2]).collect(Collectors.joining("\n")));
                return -1;
            }
            return missingPacket; // otherwise return the missing packet
        } else { // if not last packet just add to responses
            responses.add(serverResponse);
            return -1;
        }
    }

    private static int verifyResponse(String[] responseSplit) {
        outer:
        for (int i = 0; i < Integer.parseInt(responseSplit[2]); i ++){ // response split[2] is the amount of packets sent, zero indexed
            for (String res: responses){
                String [] resSplit = res.split(":");
               if (Integer.parseInt(resSplit[0]) == i){ // if found the packet continue to look for the next number packer
                   continue outer;
               }
            }
            return i; // if didnt find it return the missing packet
        }
        return -1; // no packet is missing
    }
}
