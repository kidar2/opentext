package com.example.opentext;


import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


public class Producer implements Runnable
{

	List<Future<?>> futures = new LinkedList<>();

	ExecutorService service;

	int consumersCount;

	long tId;

	volatile boolean isDone = false;

	ConcurrentLinkedQueue<String> lines = new ConcurrentLinkedQueue<>();      //for lines of file I choose non blocking ConcurrentLinkedQueue

	Map<String, Consumer> consumerPool = new WeakHashMap<>();

	Producer(long tId, ExecutorService service, int consumersCount)
	{
		this.tId = tId;
		this.service = service;
		this.consumersCount = consumersCount;
	}

	public void addLine(String line)
	{
		lines.add(line);
	}

	@Override
	public void run()
	{
		try
		{
			while (!Thread.currentThread().isInterrupted() && !lines.isEmpty() || !isDone)
			{
				var line = lines.poll();
				if (line == null)
				{
					Thread.sleep(100);
					continue;
				}
				var trimLine = line.trim();
				if (trimLine.isEmpty())
					continue;
				var arr = trimLine.split("\\|");
				var id = arr[0];
				var ms = Integer.parseInt(arr[1]);

				if (id.isEmpty())
				{
					//if Message ID is not present and processing time is present (|500|) then the producer needs to stop
					//producing for 500ms.  This simulates a pause in incoming messages.
					Thread.sleep(ms);
				}
				else
				{
					var consumer = consumerPool.get(id);
					if (consumer == null)
					{
						consumer = new Consumer(tId);
						consumerPool.put(id, consumer);
						consumer.addTask(line.trim(), ms);
						futures.add(service.submit(consumer));
					}
					else
					{
						synchronized (consumer)
						{
							consumer.addTask(line.trim(), ms);
							if (consumer.IsDone)
								futures.add(service.submit(consumer));
						}
					}
				}
			}

			for (var f : futures)
				f.get();               // waiting result of all consumers

			synchronized (this)
			{
				this.notifyAll();      //we notify root thread that he can continue work
			}
		}
		catch (InterruptedException ignore)
		{

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public synchronized void waitDone() throws InterruptedException
	{
		isDone = true;
		this.wait();
	}
}
