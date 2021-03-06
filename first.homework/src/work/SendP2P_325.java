package work;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by he on 17-3-17.
 */
public class SendP2P_325 {
    public static void main(String[] args){
        Scanner in = new Scanner(System.in);
        SendP2P_325 test = new SendP2P_325();

        System.out.print("请输入随机数种子： ");
        int random = in.nextInt();
        System.out.print("请输入超时阀植： ");
        int limitTime = in.nextInt();
        System.out.print("请输入第一个接受者的ip： ");
        String ip1= in.next();
        System.out.print("请输入第二个接受者的ip： ");
        String ip2= in.next();
        test.eventCreator(random,limitTime*1000,ip1,ip2);
    }

    public void eventCreator(int seed,int limitTime,String ip1,String ip2){
        Random random = new Random(seed);
        Message message = new Message();
        int counterOne = 0,counterZero = 0;
        int random1 = 0,random2 = 0;
        SocketThread socketThread1 = new SocketThread(ip1,999);
        SocketThread socketThread2 = new SocketThread(ip2,999);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Formatter f = new Formatter(System.out);
        long startTime=System.currentTimeMillis();   //获取开始时间

        System.out.println("端对端模式-send");
        f.format("%-9s %-10s %-10s %-19s\n", "NO", "random1", "random2", "timer");
        for(int i = 0; i < 20; i ++) {
            double R = random.nextDouble();
            double T = (-6) * Math.log(R) * 1000;
            try {
                Thread.sleep((long) T);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            /*
            **random1的生成
             */
            if (counterZero == 15) {
                random1 = 1;
                counterOne++;
            } else if (counterOne == 5) {
                random1 = 0;
                counterZero++;
            } else {
                if (R < 0.25) {
                    random1 = 1;
                    counterOne++;
                } else {
                    random1 = 0;
                    counterZero++;
                }
            }
            if(System.currentTimeMillis()-startTime>limitTime){
                random1 = 2;
            }
            /**
             * random2的生成
             */
            switch (random1){
                case 1:
                    random2 = random.nextInt(5) + 1;
                    break;
                case 0:
                    random2 = random.nextInt(100) + 101;
                    break;
                case 2:
                    random2 = 0;
                    break;
                default:
                    random2 = -1;
                    System.out.println("random1 is not 1/2/0");
            }
            message.setRandom1(random1);
            message.setRandom2(random2);
            message.setTimer(df.format(new Date()));
            f.format("%-10d %-10d %-10d %-20s \n",i + 1,message.getRandom1(),message.getRandom2(),message.getTimer());

            switch (random1){
                case 0:
                    socketThread1.setMessage(message);
                    socketThread1.run();
                    break;
                case 1:
                    socketThread2.setMessage(message);
                    socketThread2.run();
                    break;
                case 2:
                    socketThread1.setMessage(message);
                    socketThread1.run();
                    socketThread2.setMessage(message);
                    socketThread2.run();
                    break;
                default:
                    System.out.println("random1 is not 1/2/0");
            }
            if(random1 == 2){
                break;
            }
        }
        socketThread1.socketClose();
        socketThread2.socketClose();
        System.out.println("send END");
        if(random1 == 2){
            System.out.println("reason: random1 == 2");
        }
    }
}
