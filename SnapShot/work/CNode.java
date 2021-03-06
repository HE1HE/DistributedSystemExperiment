
package work;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CNode{
	private ThreadPoolExecutor tPoolExecutor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
	private ReceiveThreadManager receive;
	static String ip_i, ip_j, ip_k;
	private Socket socket_i, socket_j, socket_k;
	private ObjectOutputStream oos_i, oos_j, oos_k;
	private Event events[] = null;
	CNodeSnapshot cNodeSnapshot[];

	Formatter fPrint = new Formatter(System.out);
	String formatStr = "%-30s %-30s\n";

	public CNode(String ip1, String ip2, String ip3) {
		ip_i = ip1;
		ip_j = ip2;
		ip_k = ip3;
		fPrint.format(formatStr,"标准快照","接收到的快照");
		start_receive();
		set_callbackc();
	}
	private static int ijkIndex(char a){
		char ijk[] = {'i','j','k'};
		for(int i=0;i<ijk.length;i++){
			if(a == ijk[i]){
				return i;
			}
		}
		return -1;
	}
	public void setEvents(int source_times, int snapshot_times, int randomSeed){
		this.events = new Event[source_times + snapshot_times + 1];
		this.cNodeSnapshot = new CNodeSnapshot[snapshot_times];

		Random random = new Random(randomSeed);
		double R;
		double T;
		double source_times_rate = (double)source_times/(source_times + snapshot_times);
		long timeSum = 0;
		//产生所有事件
		char nodes[] = {'i','j','k'};
		for(int i = 0,j = 0, k = 0; k < source_times + snapshot_times; k++){
			R = random.nextDouble();
			T = -Math.log(R) * 5000;
			timeSum = timeSum + (long) T;
			Random random1 = new Random((long)T);
			if((R < source_times_rate || j >= snapshot_times) && i < source_times){
				// 资源转移事件
				char sendNode = nodes[random1.nextInt(3)];
				char recNode = 'i';
				int sourceNum = 10;
				if(sendNode == 'i'){
					char nodes1[] = {'j','k'};
					recNode = nodes1[random1.nextInt(2)];
				} else if(sendNode == 'j'){
					char nodes1[] = {'i','k'};
					recNode = nodes1[random1.nextInt(2)];
				} else if(sendNode == 'k'){
					char nodes1[] = {'i','j'};
					recNode = nodes1[random1.nextInt(2)];
				}
				this.events[k] = new Event(sendNode, recNode,10, timeSum);
				i++;
			} else if(j < snapshot_times){
				// 快照事件
				this.events[k] = new Event(String.valueOf(j),nodes[random1.nextInt(3)], 'c',timeSum);
				this.cNodeSnapshot[j] = new CNodeSnapshot(String.valueOf(j));
				j++;
			}
		}

//        this.events[0] = new Event('k','i',63,0);
//        this.events[1] = new Event('k','i',7,1574);
//        this.events[2] = new Event('j','i',62,2930);
//        this.events[3] = new Event('k','j',14,15502);
//        this.events[4] = new Event("1",'i','c',19970);
//        this.cNodeSnapshot[0] = new CNodeSnapshot(String.valueOf(1));
//        this.events[5] = new Event('j','k',42,20701);
//        this.events[6] = new Event("2",'j','c',34482);
//        this.cNodeSnapshot[1] = new CNodeSnapshot(String.valueOf(2));
//        this.events[7] = new Event('j','i',7,35010);
//        this.events[8] = new Event('k','j',7,38325);
//        this.events[9] = new Event('i','j',233,41779);

        this.setcNodeSnapshot();
	}
	private void setcNodeSnapshot(){
		PriorityQueue<Event> eventQueue = new PriorityQueue<Event>(this.events.length, new ComparatorBytime());
		//把事件加入到优先级队列
		for(int i = 0; i < this.events.length -1; i++){
			eventQueue.add(this.events[i]);
		}

		//模拟快照算法
		int ijkSourceVal[] = {300,300,300};
		Event currentEvent = eventQueue.poll();
		long currentTime = 10000;
		char sendNodeTmp;
		char recNodeTmp;
		while (currentEvent != null) {
		    currentTime = currentEvent.getWaitTime();
//			System.out.println("time:" + currentEvent.getWaitTime() +
//					"  sendId:" + currentEvent.getSendNode() + "  action:" + currentEvent.getSourceAction() +
//					"  code:" + currentEvent.getCode());
			String code[] = currentEvent.getCode().split("\\|");

			int codeHead = Integer.valueOf(code[0]);
			switch (codeHead){
				case 1:
					//资源转移事件
                    sendNodeTmp = currentEvent.getSendNode();
					char sourceAction = currentEvent.getSourceAction();
					recNodeTmp = code[1].charAt(0);
					int sourceVal = Integer.valueOf(code[2]);
					//更新资源量
					if(sourceAction == 's'){
						ijkSourceVal[ijkIndex(sendNodeTmp)] -= sourceVal;
						eventQueue.add(new Event(sendNodeTmp,
								recNodeTmp, sourceVal,
								currentEvent.getWaitTime() +
										IConstant.ijkdelay[ijkIndex(sendNodeTmp)][ijkIndex(recNodeTmp)],'r'));
					} else {
						ijkSourceVal[ijkIndex(recNodeTmp)] += sourceVal;
						//更新快照
						for(CNodeSnapshot element:this.cNodeSnapshot){
							if(element != null) {
								element.sourceEvent(sendNodeTmp, recNodeTmp, sourceVal);
							}
						}
					}

					break;
				case 2:

					//快照事件
					char snapLastNode = currentEvent.getSnapPrevNode();
                    sendNodeTmp = currentEvent.getSendNode();
					String snapID = code[1];
					int nodeSourceVal = ijkSourceVal[ijkIndex(sendNodeTmp)];
					//快照
					String strTmp[];
					for(CNodeSnapshot element:this.cNodeSnapshot){
						if(element.isSnapshotId(snapID)){
                            strTmp = element.snapshotEvent(snapLastNode, sendNodeTmp, nodeSourceVal);
							if(strTmp != null){
								eventQueue.add(
										new Event(snapID,
                                                strTmp[0].charAt(0),
                                                strTmp[4].charAt(0),
												currentEvent.getWaitTime() + Integer.valueOf(strTmp[1]))
								);
								eventQueue.add(
										new Event(snapID,
                                                strTmp[2].charAt(0),
                                                strTmp[4].charAt(0),
												currentEvent.getWaitTime() + Integer.valueOf(strTmp[3]))
								);
							}
							break;
						}
					}
					break;
				case 5:
					break;
			}
			currentEvent = eventQueue.poll();
		}
		this.events[this.events.length - 1] = new Event(currentTime + 1000);

		for(CNodeSnapshot snapshot:this.cNodeSnapshot){
			if(snapshot != null){
				System.out.println(snapshot.getStandardSnapShot());
			}
		}
	}
	private void start_send() {
		try {
			socket_i = new Socket(ip_i, IConstant.portp);
			socket_j = new Socket(ip_j, IConstant.portp);
			socket_k = new Socket(ip_k, IConstant.portp);
			oos_i = new ObjectOutputStream(socket_i.getOutputStream());
			oos_j = new ObjectOutputStream(socket_j.getOutputStream());
			oos_k = new ObjectOutputStream(socket_k.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		long lastTime = 0;
		for(Event event:this.events){
//			fPrint.format(formatStr, event.getSendNode(),event.getCode());
		    try {
				Thread.sleep(event.getWaitTime() - lastTime);
                lastTime = event.getWaitTime();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.send_event(String.valueOf(event.getSendNode()),event.getCode());
		}
	}
	private void set_callbackc() {
		CallBackManager.setCallBackc(new ICallBack() {
			
			@Override
			public void receive_handler(String node, String msg) {
				String[] src = msg.split("\\|");
//				fPrint.format(formatStr," ",msg);
				for(CNodeSnapshot snapshot:cNodeSnapshot) {
					if (snapshot.isSnapshotId(src[0])) {
						if (snapshot.setRecSnapSource(node.charAt(0), msg)) {
							fPrint.format(formatStr, snapshot.getStandardSnapShot(),
                                    snapshot.getRecSnapShot());
						}
					}
				}
			}
		});
	}

	
	private void send_event(String node, String msg) {
		/*在main函数中调用该函数向node，发送消息msg*/
		if (node.equals("i")) {
			tPoolExecutor.execute(new Send(oos_i, msg, 0));
		}else if(node.equals("j")){
			tPoolExecutor.execute(new Send(oos_j, msg, 0));
		}else if(node.equals("k")){
			tPoolExecutor.execute(new Send(oos_k, msg, 0));
		}else{
			tPoolExecutor.execute(new Send(oos_i, msg, 0));
			tPoolExecutor.execute(new Send(oos_j, msg, 0));
			tPoolExecutor.execute(new Send(oos_k, msg, 0));
		}
	}
	
	private void start_receive() {
		receive = new ReceiveThreadManager(IConstant.portc, true);
		new Thread(receive).start();
	}

	public static void main(String[] args) {
		/*接受输入:
			ip结点的IP
			source_times资源转移数
			snapshot_times快照次数
			R随机种子
		*/

		Scanner in = new Scanner(System.in);
		String ip[] = new String[3];
		int source_times;
		int snapshot_times;
        int randomSeed;
//		System.out.print("请输入i的ip： ");
//		ip[0] = in.next();
//		System.out.print("请输入j的ip： ");
//		ip[1] = in.next();
//		System.out.print("请输入k的ip： ");
//		ip[2] = in.next();
//		System.out.print("资源转移次数：");
//		source_times = in.nextInt();
//		System.out.print("快照次数：");
//		snapshot_times = in.nextInt();
//		System.out.print("随机数种子：");
//		randomSeed = in.nextInt();
		ip[0] = "223.3.104.179";
		ip[1] = "223.3.114.90";
		ip[2] = "223.3.110.81";
		source_times = 5;
		snapshot_times = 1;
		randomSeed = 43;
		CNode cNode = new CNode(ip[0], ip[1], ip[2]);
        cNode.setEvents(source_times, snapshot_times, randomSeed);
		System.out.print("输入y启动send： ");
		String make_sure = in.next();
		if (!make_sure.equals("y")) {
			return;
		}
		cNode.start_send();
        cNode.end_all();
        System.out.println("end");
	}

	private void end_all() {
		while (tPoolExecutor.getActiveCount() != 0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {

			socket_i.close();
			socket_j.close();
			socket_k.close();
			oos_i.close();
			oos_j.close();
			oos_k.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		tPoolExecutor.shutdown();
		receive.closeAllThread();

	}

}