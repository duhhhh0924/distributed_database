package ZOOKEEPERMANAGER;

import CATALOGMANAGER.CatalogManager;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ZookeeperManager {
    static final String zookeeperServer = "127.0.0.1";
    static CuratorFramework client;
    static ArrayList<String> tableNames;
    static String regionServerIP;
    static String path = "";
    private static final String masterServerName = "127.0.0.1";
    private static final int masterPort = 8002;

    /**
     * @author zzy
     * @date 2021/6/7
     * 连接zookeeper，并且创建一个serv节点
     */
    public static void zookeeperConnect(){
        tableNames = CatalogManager.get_tables();
        System.out.println(tableNames);
        try {
            regionServerIP = String.valueOf(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        path = getRegionName();
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000,10);
        client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperServer)
                .retryPolicy(retryPolicy)
                .namespace("service")
                .build();
        client.start();
        String data = regionServerIP;
        for(String t : tableNames){
            data += " ";
            data += t;
        }
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void tableChange(){
        String data = regionServerIP;
        tableNames = CatalogManager.get_tables();
        System.out.println(tableNames);
        for(String t : tableNames){
            data += " ";
            data += t;
        }
        try {
            client.setData().forPath(path, data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void connectClose(){
        client.close();
    }

    public static String getRegionName() {
        try
        {
            Socket client = new Socket(masterServerName, masterPort);
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.writeUTF(regionServerIP);
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            String name = in.readUTF();
            client.close();
            return name;
        }catch(IOException e)
        {
            e.printStackTrace();
            return "error";
        }
    }
}