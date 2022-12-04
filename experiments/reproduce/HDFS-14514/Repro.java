import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.RemoteIterator;
import java.net.URI;
import java.nio.ByteBuffer;

class Repro {
    public static void main(String args[]) throws Exception {

        String hdfsuri = "hdfs://localhost:9000";
        Path folderPath = new Path("/dataenc");
        String fileName = "a.txt";
        Path filePath = new Path(folderPath, fileName);

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsuri);
        String content;

        System.out.println("Connecting to server " + hdfsuri);
        FileSystem fs = FileSystem.get(URI.create(hdfsuri), conf);

        // 3. keep a file open for write under /dataenc
        System.out.println("Creating a file at " + filePath);
        FSDataOutputStream outf = fs.create(filePath);

        // 4. Append the file several times using the client, keep the file open
        content = "hello ";
        System.out.format("Appending file content \"%s\", and leave file open.\n", content);
        outf.writeBytes(content);
        outf.flush();
        outf.hsync();
        Thread.sleep(1000);

        // 5. Create a snapshot
        System.out.println("Creating a snapshot for " + folderPath);
        Path snapshotFolder = fs.createSnapshot(folderPath, "snap1");
        System.out.println("Created a snapshot at " + snapshotFolder);

        // 6. Append the file one or more times, but don't let the file size
        // exceed the block size limit. Wait for several seconds for the append
        // to be flushed to DN.
        content = "world";
        System.out.format("Appending file content \"%s\", and leave file open.\n", content);
        outf.writeBytes("world");
        outf.flush();
        outf.hsync();
        // sleep 5s
        System.out.println("Sleeping for 5 seconds...");
        Thread.sleep(5000);

        // 7.1 Do a -ls on the file inside the snapshot
        long listedFileSize = -1;
        System.out.println("Listing files in snapshot folder " + snapshotFolder);
        for (RemoteIterator<LocatedFileStatus> it = fs.listFiles(snapshotFolder, false); it.hasNext(); ) {
            LocatedFileStatus info = it.next();
            System.out.println(info);
            if (info.getPath().getName().equals(fileName)) {
                listedFileSize = info.getLen();
            }
        }
        System.out.println("Finished listing files.");
        if (listedFileSize != -1)
            System.out.format("Got listed file size for %s: %d\n", fileName, listedFileSize);
        else
            System.out.format("File %s not found!\n", fileName);

        // 7.2 then try to read the file using -get
        System.out.println("Closing the file...");
        outf.close();
        System.out.println("Closed the file.");

        Path snapshotFilePath = new Path(snapshotFolder, fileName);
        System.out.println("Now read the file at " + snapshotFilePath);
        FSDataInputStream inf = fs.open(snapshotFilePath);
        ByteBuffer buf = ByteBuffer.allocate(1024);

        int bytesRead = inf.read(buf);
        inf.close();
        System.out.format("Read %d bytes, content \"%s\"\n", bytesRead, new String(buf.array(), "ASCII"));

        // 7.3 you should see the actual file size read is larger than the
        // listing size from -ls.
        System.out.format("Length in ls is %d, length in read is %d.\n", listedFileSize, bytesRead);
        if (listedFileSize == bytesRead)
            System.out.println("Equal!");
        else
            System.out.println("Not Equal!");
    }
}
