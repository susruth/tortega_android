package io.lambdaprotocol.tortega;


import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by susruth on 28/11/17.
 */

public class Account {


    private URL ROPSTEN;
    private URL MAINNET;
    private ECKey privateKey = ECKey.fromPrivate(hexStringToByteArray("1bf210f922183dbe3b1bde07aeb08b20719775145c52208f503994ec9c361942"));
    private String publicAddress = "0x"+bytesToHex(privateKey.getAddress());

    public Account(){
        try{
            ROPSTEN = new URL("https://ropsten.infura.io");
            MAINNET = new URL("https://mainnet.infura.io");
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public String send(String payload) throws Exception {
        try{
            Transaction t = build(payload);
            t.sign(privateKey);
            return "hash: "+submitTx("0x"+bytesToHex(t.getEncoded()));
        }catch (Exception e){
            throw e;
        }
    }

    public Transaction build(String payload) throws Exception{
        try{
            JSONObject obj = new JSONObject(payload);
            byte[] nonce = hexStringToByteArray(getNonce().split("x")[1]);
            byte[] gasPrice = hexStringToByteArray(getGasPrice().split("x")[1]);
            byte[] gasLimit = hexStringToByteArray("5208");
            byte[] value = hexStringToByteArray(obj.optString("value").split("x")[1]);
            byte[] data = hexStringToByteArray("0");
            byte[] to = hexStringToByteArray(obj.optString("to").split("x")[1]);
            return new Transaction(nonce,gasPrice,gasLimit,to,value,data,3);
        }catch (Exception e){
            throw e;
        }
    }

    public String post(String payload) throws Exception {
        JSONObject result;
        try{
            result = new JSONObject(httpPost(ROPSTEN,payload));
            return result.optString("result");
        }catch(Exception e){
            throw e;
        }
    }

    public String submitTx(String stx) throws Exception {
        String payload = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_sendRawTransaction\",\"params\":[\""+stx+"\"],\"id\":73}";
        return post(payload);
    }

    public String getNonce() throws Exception {
        String payload = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getTransactionCount\",\"params\":[\""+this.publicAddress+"\",\"latest\"],\"id\":1}";
        return post(payload);
    }

    public String getGasPrice() throws Exception {
        String payload = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_gasPrice\",\"params\":[],\"id\":73}";
        return post(payload);
    }


//    public String getGasLimit(String to, String data, String value, String gasPrice ) throws Exception {
//        String payload = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_estimateGas\",\"params\":[{" +
//                "\"from\":\"" + this.publicAddress + "\","+
//                "\"to\":\"" + to + "\"" +
//                ","+ "\"gasPrice\":\"" + gasPrice + "\","+
//                "\"value\":\"" + value + "\"" +
//                "\"data\":\"" + data + "\"}],\"id\":1}";
//        return post(payload);
//    }


    private String httpPost(URL url, String payload) throws Exception {
        String response = "";
        try {
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            writeStream(out, payload);
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                throw new Exception("Connection to the url Failed");
            }
            urlConnection.disconnect();
        } catch (Exception e) {
            throw e;
        }
        return response;
    }
    private void writeStream(OutputStream out, String data) {
        try{
            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public static BigInteger twosComplement(BigInteger original)
    {
        byte[] contents = original.toByteArray();
        byte[] result = new byte[contents.length + 1];
        System.arraycopy(contents, 0, result, 1, contents.length);
        result[0] = (contents[0] < 0) ? 0 : (byte)-1;
        return new BigInteger(result);
    }

    public static String bytesToHex(byte[] bytes) {
        BigInteger zero =  BigInteger.ZERO;
        BigInteger b =  new BigInteger(bytes);
        if(b.compareTo(zero) < 0) {
            b = twosComplement(b);
        }
        return b.toString(16);
    }

    public static byte[] hexStringToByteArray(String s) {
        byte zero = Byte.parseByte("0",16);
        byte[] bytes = new BigInteger(s,16).toByteArray();
        if (bytes[0] == zero){
            byte[] bytes2 = new byte[bytes.length - 1];
            for (int i=1; i < bytes.length; i++){
                bytes2[i-1] = bytes[i];
            }
            bytes = bytes2;
        }
        return bytes;
    }

}

