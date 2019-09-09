import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;



//class which hold all request of clients
class AllRequest 
{
    public int ClientId;
    public String Process;
    public int PageId;
    public char Content[];
}


// This method hold all buffer data
class Buffer 
{
    public int PageId;
    public char[] Content;
}



// Server class is used to process the request issued by client in form of files and it implement the logic of request processing
class Server extends Thread 
{

    AllRequest req;
    private File dataFile;
    public static List<Buffer> init_buffer;
    public static List<Buffer> pool;

    int bufSize;
    public static List<AllRequest> allRequestList;

	
	
	
	
	// Server constructor to initialize the buffer
		Server(int size) throws IOException 
		{
        init_buffer = new ArrayList<Buffer>();
        pool = new ArrayList<Buffer>();
        bufSize = size;
        allRequestList = new ArrayList<AllRequest>();
        Readfile("Resources/init_buffer_pages.dat", 0);
        Readfile("Resources/all_requests.dat", 1);

        for(int i =0; i< bufSize; i++)
        {
            pool.add(init_buffer.get(i));
        }
    }

	
	
	// Run method that run the server thread
		
    @Override
    public void run() 
	{
        while (!allRequestList.isEmpty()) 
		{
            req = allRequestList.get(0);

            if (req.Process.toUpperCase().equals("READ")) 
			{
                ReadRequest(req.ClientId, req.PageId);
            } else {
                WriteRequest(req.ClientId, req.PageId, req.Content);
            }

            allRequestList.remove(req);
        }
    }

	
	
	// The request method which process write requests
	    public synchronized void WriteRequest(int cid, int pageId, char[] content) 
	{
        int flag = 0;
        
        for (int i = 0; i< pool.size(); i++) 
		{
			Buffer item = pool.get(i);
            if (item.PageId == pageId) 
			{
                item.Content = content;
                System.out.println("Client Id " + cid + " Write Page " + pageId +" Content : " + String.valueOf(item.Content) + "at Page Id:" + item.PageId);               

                flag = 1;
				shiftItem(i, item, pool);
				break;
            }
        }

        if(flag == 0)
        {
            for (int i = 0; i< init_buffer.size(); i++) 
			{
            {
				Buffer item = init_buffer.get(i);
                if (item.PageId == pageId) 
				{
					flag = 1;
                    item.Content = content;
					
					try {
						CreateLog("Resources/results/server_log.log", 0, pool.get(0).PageId, String.valueOf(pool.get(0).Content));
					} catch (Exception e) 
					{
						System.out.println("Failed to read file due to following reason: /n" + e);
					}

                    shiftItem(0, item, pool);                    
					break;
                }
            }
        }
    }}
	
	
	public synchronized void shiftItem(int index, Buffer item, List<Buffer> pool) 
	{
		for (int j = index; j< pool.size(); j++) 
		{
			if(j+1 < pool.size())			
				pool.set(j, pool.get(j+1));				
			else
				pool.set(j, item);
		}
	}
		

	
	
	// The read Request method which process read requests
		public synchronized void ReadRequest(int cid, int pageId) 
		{
        int flag = 0;
        for (int i = 0; i< pool.size(); i++) 
		{
			Buffer item = pool.get(i);
            if (item.PageId == pageId) 
			{
                System.out.println("Client Id " + cid + " Read " + pageId +" Content : " + String.valueOf(item.Content));                
                try {
                    CreateLog("Resources/results/client_log_" + cid + ".log", cid, item.PageId, String.valueOf(item.Content));
                } catch (Exception e) 
				{
                    System.out.println("Failed to read file due to following reason: /n" + e);
                }

                flag = 1;
				shiftItem(i, item, pool);
				break;
            }
        }

        if(flag == 0)
        {
            for (int i = 0; i< init_buffer.size(); i++) 
			{
            {
				Buffer item = init_buffer.get(i);
                if (item.PageId == pageId) {
                    System.out.println("Client Id " + cid + " Read " + pageId +" Content : " + String.valueOf(item.Content));
                    try 
					{
                        CreateLog("Resources/results/client_log_" + cid + ".log", cid, item.PageId, String.valueOf(item.Content));
						CreateLog("Resources/results/server_log.log", 0, pool.get(0).PageId, String.valueOf(pool.get(0).Content));
                    } catch (Exception e) 
					{
                        System.out.println("Failed to read file due to following reason: /n" + e);
                    }

                    shiftItem(0, item, pool);                    
					break;
                }
            }
        }
    }
	}

	
	
	// This method will create read log of request
	   public synchronized void CreateLog(String path, int cid, int pageId, String content) throws IOException 
	   {
        FileWriter f0 = new FileWriter(path, true);
        f0.write(pageId + " " + content + "\n");
        f0.close();
    }

	
	
	// The method will load file data into objects
	    public void LoadFileData(int typeOfTask) throws IOException 
		{
        Scanner input = new Scanner(dataFile);

        if (typeOfTask == 0) 
		{
            while (input.hasNextLine()) 
			{
                Buffer item = new Buffer();
                item.PageId = input.nextInt();
                item.Content = new char[4096];
                item.Content = input.next().toCharArray();
                init_buffer.add(item);
            }
        } 
		else 
		{
            while (input.hasNextLine()) 
			{
                AllRequest item = new AllRequest();
                item.ClientId = input.nextInt();
                item.Process = input.next();
                item.PageId = input.nextInt();

                if (!item.Process.toUpperCase().equals("WRITE")) 
				{
                    item.Content = new char[0];
                } else {
                    item.Content = new char[4096];
                    item.Content = input.next().toCharArray();
                }

                allRequestList.add(item);
            }
        }
    }

	
	
	// This method to issued a read request to scanner
	    private void Readfile(String path, int taskComplte) throws IOException 
	{
        this.dataFile = new File(path);
        LoadFileData(taskComplte);
    }
}



	//Client class is used to define client thread and manage client works
    public class Client extends Thread 
    {
    
    Server serVar;

    
	
	// Client constructor to initialize objects.
       public Client(Server serve) 
	   {
        this.serVar = serve;
       }

	
	// This method which run the server thread
		
    @Override
    public void run() {
        
    }

	//This method define the main methos		
    public static void main(String[] args) throws IOException 
	{
        int bufSize = Integer.parseInt(args[1]);
        Server server = new Server(bufSize);
        server.start();

        int noOfThread = Integer.parseInt(args[0]);
        for (int i = 0; i < noOfThread; i++) 
		{
            Client thread = new Client(server);
            thread.setName("Client" + i);
            thread.start();

            try {
                thread.join();
            } catch (Exception e) 
			{
                System.out.println("error");
            }
        }
    }
}



