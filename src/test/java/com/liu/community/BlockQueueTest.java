package com.liu.community;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 理解阻塞队列
 */
public class BlockQueueTest {

    public static void main(String[] args) {
        //生成一个队列
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);

        //一个生产者产生数据 生产满了容量 阻塞，等待消费
        new Thread(new Producer(queue)).start();
        //三个消费者消费数据 消费完了数据 阻塞，等待生产
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
    }

}
//生产者
class Producer implements Runnable{

    private BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i=0;i<100;i++){
                Thread.sleep(20);
                queue.put(i);
                System.out.println(Thread.currentThread().getName()+"在生产"+i+"|队列中的数据大小："+queue.size());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
//消费者
class Consumer implements Runnable{

    private BlockingQueue<Integer> queue;

    public Consumer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true){
                Thread.sleep(new Random().nextInt(1000));
                int take = queue.take();
                System.out.println(Thread.currentThread().getName()+"在消费"+take+"|队列中的数据大小："+queue.size());
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}