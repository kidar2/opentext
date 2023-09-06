package com.example.opentext;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Consumer implements Runnable
{
	private final long prodThreadId;


	private final ConcurrentLinkedQueue<MicroTask> tasks = new ConcurrentLinkedQueue<>();

	public Consumer(long prodThreadId)
	{
		this.prodThreadId = prodThreadId;
	}

	public void addTask(String line, int ms)
	{
		tasks.add(new MicroTask(line, ms));
	}

	public volatile boolean IsDone = false;


	@Override
	public void run()
	{
		try
		{
			while (!tasks.isEmpty())
			{
				IsDone = false;
				for (MicroTask task = tasks.poll(); task != null; task = tasks.poll())
				{
					task.execTask(prodThreadId);
				}
				synchronized (this)
				{
					IsDone = true;
				}
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

	private static class MicroTask
	{
		private final String line;
		private final int ms;

		public MicroTask(String line, int ms)
		{
			this.line = line;
			this.ms = ms;
		}

		public void execTask(long prodThreadId) throws InterruptedException
		{

			Date startReadTime = new Date();

			//The second value is how long it takes to process the message (in milliseconds) and the third value is the payload.
			//none of the values will contain the delimiter so you can assume there will at most be two '|' characters per line.
			Thread.sleep(this.ms);


			//Wait Time - represents the number of milliseconds between the time the producer read the message to when the consumer started to process it.
			var start = new Date();
			var wt = start.getTime() - startReadTime.getTime();

			SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
			System.out.printf("PID: %s;  %s;  Thread: %s;  Start: %s;  End: %s;  Wait Time (ms): %s \n",
					  prodThreadId, line, Thread.currentThread().getId(), format.format(start),
					  format.format(new Date()), wt);
		}

	}
}
