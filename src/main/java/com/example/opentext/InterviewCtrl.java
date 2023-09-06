package com.example.opentext;

import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

@RestController
@RequestMapping("/interview")
public class InterviewCtrl
{


	@PostMapping("/process-file/{cnt}")
	public void processFile(@PathVariable int cnt, @RequestBody String fileContent)
	{
		if (fileContent == null || fileContent.isEmpty() || cnt < 1)
			return;

		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");

		var tId = Thread.currentThread().getId();
		System.out.printf("PID: %d;  START: %s;  Consumers: %d;  File: messages.txt\n", tId, format.format(new Date()), cnt);

		ExecutorService service = Executors.newFixedThreadPool(cnt + 1);		// +1 for producer thread
		var producer = new Producer(tId, service, cnt);
		service.submit(producer);

		try
		{
			var lines = fileContent.split("\n");

			//Thread.sleep(1000);

			for (var line : lines)
				producer.addLine(line);

			producer.waitDone();		//wait while all tasks will do. Is not the flag of end of file. It's just for correct time when we can shutdown ExecutorService and return result of post request.
											//Yes in real app we don't know when the file is end, but in this case we have to know when I can print the "END LINE"
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			service.shutdown();

			System.out.printf("PID: %d;  END: %s", tId, format.format(new Date()));			//"END LINE"
		}
	}
}
