package com.example.smbupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import android.os.Bundle; 
import android.os.Environment;
import android.provider.MediaStore.Files;
import android.app.Activity; 
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent; 
import android.graphics.Path;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText; 
import android.widget.Toast;
import jcifs.smb.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.example.smbupload.R;

import android.os.MemoryFile;  







import com.example.smbupload.Property;


public class SmbUploadActivity extends Activity {

	class fileInfo {
		int NumOfFiles;
		int SizeOfFiles;
	}

	private static final int REQUEST_PATH_SMB = 1;
	private static final int REQUEST_PATH_NFS = 2;
	private static final int PROGRESS_DIALOG = 0;
	public static final String ENCODING = "UTF-8";
	private static final int MENU_SET_HOME_DIR   = Menu.FIRST,
								MENU_SET_SMB_SERVER = Menu.FIRST+1,
								MENU_SET_NFS_SERVER = Menu.FIRST+2;
	static final int SizeOfOneMb = 1024*1024;
	private String HALLOWEEN_ORANGE = "#FF7F27";

	String mCurFileName;
	String mCurPath;
	String mSelectFolderPath;
	String mCurSmbUrl;
	String mCurUserName;
	String mCurPassword;
	
	String mCurNfsIP;
	String mCurNfsSharePath;
	String mCurNfsLocalPath;
	boolean bNFSMounted = false;
	
	int Option = 0;
	
	Property mProperty=null;
	boolean bSmbUrlAccess = false;
	NtlmPasswordAuthentication mAuth;
	SmbFile mSmbFile;
	EditText edittext;
	int mFolderFiles;
	int mFilesUploaded;
	int mCounter;
	int mFolderSize;
    private ProgressDialog progress;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smbupload); 
        //edittext = (EditText)findViewById(R.id.editText);
        progress = new ProgressDialog(this);
        mProperty = new Property();
		mCurSmbUrl = readFileData("SMBURL");
		mCurUserName = readFileData("SMBUSERNAME");
		mCurPassword = readFileData("SMBPASSWORD");
		
		mCurNfsIP = readFileData("NFS_IP");
		mCurNfsSharePath = readFileData("NFS_SHARE_PATH");
		mCurNfsLocalPath = readFileData("NFS_LOCAL_PATH");

		Intent i = new Intent("com.example.mountnfs.MOUNT");
		Bundle bundle = new Bundle();
		bundle.putString("IP_ADDRESS", 		mCurNfsIP);
		bundle.putString("SHARE_FOLDER", 	mCurNfsSharePath);
		bundle.putString("LOCAL_PATH", 		mCurNfsLocalPath);
		i.putExtras(bundle);
		sendBroadcast(i);

		testURLAccess();
    }
	
//	@Override
//	protected void onPause() {
//		// TODO Auto-generated method stub
//		if(mProperty.CheckMount(mCurNfsIP, mCurNfsSharePath, mCurNfsLocalPath))
//		{
//	    	// If Smb server cannot access, then it can not get file to upload.
//	    	Intent i = new Intent("com.example.mountnfs.UMOUNT");
//	    	Bundle bundle = new Bundle();
//	    	bundle.putString("UMOUNT_PATH", mCurNfsLocalPath);
//	    	i.putExtras(bundle);
//	    	sendBroadcast(i);
//		}
//	}

    public void writeFileData(String filename, String message){
        try {
           FileOutputStream fout = openFileOutput(filename, MODE_PRIVATE);

           byte[]  bytes = message.getBytes();
           fout.write(bytes);
           fout.close();
       } catch (Exception e) {
           e.printStackTrace();
       }
    }
    public String readFileData(String fileName){
        String result="";
        try {
           FileInputStream fin = openFileInput(fileName);
           int lenght = fin.available();
           byte[] buffer = new byte[lenght];
           fin.read(buffer);
           result = new String(buffer, "UTF-8");
       } catch (Exception e) {
           e.printStackTrace();
       }
       return result;
    }

    public boolean SmbCreateFolder(String path)
    {
        String name=mCurUserName;
        String password=mCurPassword;
        String SmbUrl = mCurSmbUrl;
        String relatePath = "";

        if((path.length()-mSelectFolderPath.length())!=0)
        {
        	relatePath = path.substring(mSelectFolderPath.length()+1,path.length());
        }
        if(relatePath=="")
        {
        	relatePath = mCurFileName;
        }
        else
        {
        	relatePath = mCurFileName + "/" +relatePath;
        }
        if(mCurSmbUrl.substring(mCurSmbUrl.length()-1)!="/")
        {
        	SmbUrl = SmbUrl + "/";
        }
        String url = SmbUrl + relatePath;
        try {
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null,name,password);
			SmbFile serverFile = new SmbFile(url, auth);
			serverFile.mkdir();
			//serverFile.setReadWrite();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SmbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return true;
    }
    
    public void SmbUploadFolder(String Path)
    {
    	File localFile = new File(Path);
    	File[] FileList = localFile.listFiles();
    	for(int i=0;i<FileList.length;i++)
    	{
    		if(FileList[i].isDirectory())
    		{
    			SmbCreateFolder(FileList[i].getPath());
    			SmbUploadFolder(FileList[i].getPath());
    		}
    		else
    		{
    			SmbUploadFile(FileList[i].getPath(),FileList[i].getName(),true);
    			//mFilesUploaded = mFilesUploaded + 1;
    			//progress.setProgress(mFilesUploaded);
    		}
    	}
    }
    
    public fileInfo LocalFileCounter(String Path)
    {
    	fileInfo Info = new fileInfo();
    	Info.NumOfFiles = 0;
    	Info.SizeOfFiles = 0;
    	File localFile = new File(Path);
    	File[] FileList = localFile.listFiles();
    	for(int i=0;i<FileList.length;i++)
    	{
    		if(FileList[i].isDirectory())
    		{
    			fileInfo SubInfo = LocalFileCounter(FileList[i].getPath());
    			Info.NumOfFiles = Info.NumOfFiles + SubInfo.NumOfFiles;
    			Info.SizeOfFiles= Info.SizeOfFiles+ SubInfo.SizeOfFiles;
    		}
    		else
    		{
    			Info.NumOfFiles = Info.NumOfFiles + 1;
    			Info.SizeOfFiles= Info.SizeOfFiles + (int)FileList[i].length();
    		}
    	}
    	return Info;
    }

    public boolean SmbUploadFile(String localPath,String fileName,boolean bNeedRelatePath) {
        try {        	
            String name=mCurUserName;
            String password=mCurPassword;
            String SmbUrl = mCurSmbUrl;
            String relatePath = "";
            String url = null;
            if("/".equals(mCurSmbUrl.substring(mCurSmbUrl.length()-1))==false)
            {
            	SmbUrl = SmbUrl + "/";
            }

            if(bNeedRelatePath)
            {
            	SmbUrl = SmbUrl + mCurFileName + "/";
	            if((localPath.length()-mSelectFolderPath.length())!=0)
	            {
	            	relatePath = localPath.substring(mSelectFolderPath.length()+1,localPath.length());
	            }
	            
	            if(relatePath.length()!=0)
	            {
	            	SmbUrl = SmbUrl + relatePath ;
	            }
	            url = SmbUrl;
	           
            }
            else
            {
            	url = SmbUrl + fileName;
            }
 
            
            SmbFile file = null;
            try {
                NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null,name,password);
                file = new SmbFile(url, auth);

                SmbFileOutputStream out = new SmbFileOutputStream(file);

            	FileInputStream fileInputStream=null;
            	File FILE = null;
            	if(bNeedRelatePath)
            	{
            		FILE = new File(localPath);

            	}
            	else
            	{
                    FILE = new File(localPath+fileName);
            	}
                int TotalSize = (int) FILE.length();
                fileInputStream = new FileInputStream(FILE);

                if(TotalSize>SizeOfOneMb)
                {
                	byte[] MbFile = new byte[SizeOfOneMb];
	                int Offset = 0;
	                boolean LastByte = false;
	                while((fileInputStream.read(MbFile))!=-1)
	                {
	                	out.write(MbFile);
	                	Offset = Offset + SizeOfOneMb;
	                	
	                	if((Offset+SizeOfOneMb) > TotalSize)
	                	{
	                		LastByte = true;
	                		break;
	                	}
	                	if(!bNeedRelatePath)
	                	{
	                		progress.setProgress((int) (100*((float)Offset)/((float)TotalSize)));
	                	}
	                	else
	                	{
	                		mFilesUploaded = mFilesUploaded + SizeOfOneMb;
	                		progress.setProgress(mFilesUploaded);
	                	}
	                }
	                MbFile = null;
	                if(LastByte)
	                {
	                    byte[] LastBytes = new byte[TotalSize-Offset];
	                    if(fileInputStream.read(LastBytes)!=-1)
	                    {
	                    	out.write(LastBytes);
	                    }
	                	if(!bNeedRelatePath)
	                	{
	                		progress.setProgress(100);
	                	}
	                	else
	                	{
	                		mFilesUploaded = mFilesUploaded + TotalSize-Offset;
	                		progress.setProgress(mFilesUploaded);
	                	}
	                    LastBytes = null;
	                }
                }
                else
                {
                	byte[] FileLast = new byte[TotalSize];
                	out.write(FileLast);
                	if(!bNeedRelatePath)
                	{
                		progress.setProgress(100);
                	}
                }
                if(!bNeedRelatePath)
                {
                	progress.dismiss();
                }
                if(fileInputStream!=null)
        	    {
        	    	fileInputStream.close();
        	    }

        	    if(out!=null)
        	    {
        	    	out.close();
        	    }

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
 
    protected void onProgressUpdate(String Message) {
           progress.setMessage(Message);
    }

    public void getfile(View view){ 
    	// If Smb server cannot access, then it can not get file to upload.
    	if(bSmbUrlAccess)
    	{
			Option = 0;
			Toast.makeText(getApplicationContext(), "SMB server access ok!", Toast.LENGTH_SHORT).show();
	    	Intent intent1 = new Intent(this, FileChooser.class);
			startActivityForResult(intent1,REQUEST_PATH_SMB);
    	}
    	else
    	{
			Toast.makeText(getApplicationContext(), "SMB server access fail", Toast.LENGTH_SHORT).show();
    	}
    }

    public void getfile_nfs(View view){ 

		if(mProperty.CheckMount(mCurNfsIP, mCurNfsSharePath, mCurNfsLocalPath))
		{
			Option = 1;
	    	// If Smb server cannot access, then it can not get file to upload.
			Toast.makeText(getApplicationContext(), "NFS server is mounted :"+mCurNfsLocalPath, Toast.LENGTH_SHORT).show();
	    	Intent intent1 = new Intent(this, FileChooser.class);
			startActivityForResult(intent1,REQUEST_PATH_NFS);
		}
		else
		{
			Toast.makeText(getApplicationContext(), "NFS server isn't mounting, please try again later", Toast.LENGTH_SHORT).show();
		}
    }

    
    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, MENU_SET_HOME_DIR,   0, "Set Home Dir");
		menu.add(0, MENU_SET_SMB_SERVER, 0, "Set SMB Server");
		menu.add(0, MENU_SET_NFS_SERVER, 0, "Set NFS Server");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) SmbUploadActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
		switch (item.getItemId()) {
		case MENU_SET_HOME_DIR:
			String Homedir = readFileData("HOMEDIR");
			//inflater = (LayoutInflater) SmbUploadActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			final View v1 = inflater.inflate(R.layout.set_home_menu,null);
			EditText home_dir_et = (EditText) (v1.findViewById(R.id.edittext_LocalHomeDir));
			home_dir_et.setText(Homedir);
			new AlertDialog.Builder(SmbUploadActivity.this)
		    .setTitle("HOME DIR SETTING")
		    .setView(v1)
		    .setPositiveButton("Enter", new DialogInterface.OnClickListener() 
		    {
		    	@Override
		        public void onClick(DialogInterface dialog, int which) {                               
		    		EditText homedir_et = (EditText) (v1.findViewById(R.id.edittext_LocalHomeDir));
		    		String HomeDir="";
					if("".equals(homedir_et.getText().toString().trim()))
					{
						Toast.makeText(getApplicationContext(), "HOME DIR = null, default set /mnt/sata/", Toast.LENGTH_SHORT).show();
						HomeDir = "/mnt/sata/";
					}
					else
					{
						HomeDir = homedir_et.getText().toString();
					}
					writeFileData("HOMEDIR",HomeDir);
		    	}
		    }).show();
			break;
		case MENU_SET_SMB_SERVER:
			mCurSmbUrl = readFileData("SMBURL");
			mCurUserName = readFileData("SMBUSERNAME");
			mCurPassword = readFileData("SMBPASSWORD");

			//inflater = (LayoutInflater) SmbUploadActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			final View v = inflater.inflate(R.layout.set_smb_server_menu,null);

			EditText smb_url_et = (EditText) (v.findViewById(R.id.smb_url));
			EditText user_name_et = (EditText) (v.findViewById(R.id.user_name));
			EditText password_et = (EditText) (v.findViewById(R.id.password));

			smb_url_et.setText(mCurSmbUrl);
			user_name_et.setText(mCurUserName);
			password_et.setText(mCurPassword);
			
			new AlertDialog.Builder(SmbUploadActivity.this)
		    .setTitle("SMB SERVER SETTING")
		    .setView(v)
		    .setPositiveButton("Enter", new DialogInterface.OnClickListener() 
		    {
		        @Override
		        public void onClick(DialogInterface dialog, int which) {                               
				EditText smb_url_et = (EditText) (v.findViewById(R.id.smb_url));
				EditText user_name_et = (EditText) (v.findViewById(R.id.user_name));
				EditText password_et = (EditText) (v.findViewById(R.id.password));

				
				if("".equals(smb_url_et.getText().toString().trim()))
				{
					Toast.makeText(getApplicationContext(), "Smb server url can not be null", Toast.LENGTH_SHORT).show();
					mCurSmbUrl = null;
				}
				else
				{
					mCurSmbUrl = smb_url_et.getText().toString();
				}
				
				if("".equals(user_name_et.getText().toString().trim()))
				{
					mCurUserName = null;
				}
				else
				{
					mCurUserName = user_name_et.getText().toString();
				}
				
				if("".equals(password_et.getText().toString().trim()))
				{
					mCurPassword = null;
				}
				else
				{
					mCurPassword = password_et.getText().toString();
				}

				writeFileData("SMBURL",mCurSmbUrl);
				writeFileData("SMBUSERNAME",mCurUserName);
				writeFileData("SMBPASSWORD",mCurPassword);
				testURLAccess();
                
		        }
		     })
		    .show();
			break;
		case MENU_SET_NFS_SERVER :
			mCurNfsIP = readFileData("NFS_IP");
			mCurNfsSharePath = readFileData("NFS_SHARE_PATH");
			mCurNfsLocalPath = readFileData("NFS_LOCAL_PATH");

			//inflater = (LayoutInflater) SmbUploadActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			final View v2 = inflater.inflate(R.layout.set_nfs_server_menu,null);

			EditText nfs_ip_et = (EditText) (v2.findViewById(R.id.nfs_ip));
			EditText nfs_share_et = (EditText) (v2.findViewById(R.id.nfs_share_path));
			EditText nfs_local_et = (EditText) (v2.findViewById(R.id.nfs_local_path));

			nfs_ip_et.setText(mCurNfsIP);
			nfs_share_et.setText(mCurNfsSharePath);
			nfs_local_et.setText(mCurNfsLocalPath);
			
			new AlertDialog.Builder(SmbUploadActivity.this)
		    .setTitle("NFS SERVER MOUNT SETTING")
		    .setView(v2)
		    .setPositiveButton("Enter", new DialogInterface.OnClickListener() 
		    {
		        @Override
		        public void onClick(DialogInterface dialog, int which) {                               
				EditText nfs_ip_et = (EditText) (v2.findViewById(R.id.nfs_ip));
				EditText nfs_share_et = (EditText) (v2.findViewById(R.id.nfs_share_path));
				EditText nfs_local_et = (EditText) (v2.findViewById(R.id.nfs_local_path));

				// umount the local path first.
				mCurNfsLocalPath = readFileData("NFS_LOCAL_PATH");
		    	Intent i = new Intent("com.example.mountnfs.UMOUNT");
		    	Bundle bundle = new Bundle();
		    	bundle.putString("UMOUNT_PATH", mCurNfsLocalPath);
		    	i.putExtras(bundle);
		    	sendBroadcast(i);

				if("".equals(nfs_ip_et.getText().toString().trim()))
				{
					Toast.makeText(getApplicationContext(), "Nfs IP cannot be null", Toast.LENGTH_SHORT).show();
					mCurNfsIP = null;
				}
				else
				{
					mCurNfsIP = nfs_ip_et.getText().toString();
				}
				
				if("".equals(nfs_share_et.getText().toString().trim()))
				{
					Toast.makeText(getApplicationContext(), "Nfs SharePath cannot be null", Toast.LENGTH_SHORT).show();
					mCurNfsSharePath = null;
				}
				else
				{
					mCurNfsSharePath = nfs_share_et.getText().toString();
				}

				if("".equals(nfs_local_et.getText().toString().trim()))
				{
					Toast.makeText(getApplicationContext(), "Nfs Local Path cannot be null, use default /mnt/nfs", Toast.LENGTH_SHORT).show();
					mCurNfsLocalPath = "/mnt/nfs";
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Nfs Local Path is using /mnt/nfs, it does not support other path", Toast.LENGTH_SHORT).show();
					mCurNfsLocalPath = "/mnt/nfs";//nfs_local_et.getText().toString();
				}

				writeFileData("NFS_IP",mCurNfsIP);
				writeFileData("NFS_SHARE_PATH",mCurNfsSharePath);
				writeFileData("NFS_LOCAL_PATH",mCurNfsLocalPath);
				
	        	i = new Intent("com.example.mountnfs.MOUNT");
	        	bundle = new Bundle();
	        	bundle.putString("IP_ADDRESS", mCurNfsIP);
	        	bundle.putString("SHARE_FOLDER", mCurNfsSharePath);
	        	bundle.putString("LOCAL_PATH", mCurNfsLocalPath);
	        	i.putExtras(bundle);
	        	sendBroadcast(i);
				//testURLAccess();

		        }
		     })
		    .show();
			
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void testURLAccess()
	{
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                	// Access test
                    NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null,mCurUserName,mCurPassword);
                    SmbFile file=null;
    				try {
    					String SmbUrl = mCurSmbUrl;
    		            if(mCurSmbUrl.substring(mCurSmbUrl.length()-1)!="/")
    		            {
    		            	SmbUrl = SmbUrl + "/";
    		            }
    					file = new SmbFile(SmbUrl+"test.test", auth);
    	                SmbFileOutputStream out = new SmbFileOutputStream(file);
    					if(out.isOpen())
    	                {
    						bSmbUrlAccess = true;
    						out.close();
    						file.delete();
    	                }
    	                else
    	                {
    						bSmbUrlAccess = false;
    	                }
    				} catch (MalformedURLException e) {
    					// TODO Auto-generated catch block
						bSmbUrlAccess = false;
    					e.printStackTrace();
    				} 
                } catch (Exception e) {
					bSmbUrlAccess = false;
                    e.printStackTrace();
                }
            }
        });
        thread.start();
	}

 // Listen for results.
    
	public void CopyFileToNFS(String Path,String FileName) throws FileNotFoundException
	{
		File source = new File(Path+FileName);
		File dest   = new File(mCurNfsLocalPath+"/"+FileName);
		long TotalSize = source.length();
		long offset=0;
	    InputStream is = null;
	    OutputStream os = null;
	    try 
	    {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
			while ((length = is.read(buffer)) > 0) {
			    os.write(buffer, 0, length);
			    offset = offset + length;
			    progress.setProgress((int) (((float)offset/(float)TotalSize)*100));
			}
			is.close();
			os.close();
		}	
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // See which child activity is calling us back.
    	if (Option==0){
    		if (resultCode == RESULT_OK) { 
    			mCurFileName = data.getStringExtra("GetFileName"); 
    			mCurPath = data.getStringExtra("GetPath");
            	mFolderFiles = 0;
            	mFolderSize = 0;
            	File FILE = new File(mCurPath+"/"+mCurFileName);

            	if(FILE.isDirectory())
            	{
            		mSelectFolderPath = mCurPath+"/"+mCurFileName;
            		fileInfo folderInfo = LocalFileCounter(mSelectFolderPath);
            		mFolderFiles = folderInfo.NumOfFiles;
            	    mFolderSize  = folderInfo.SizeOfFiles;
            		LayoutInflater inflater = (LayoutInflater) SmbUploadActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
        			final View v = inflater.inflate(R.layout.checkbox,null);
        			new AlertDialog.Builder(SmbUploadActivity.this)
        		    .setTitle("Upload Folder ("+mCurPath+"/"+mCurFileName+") to " + mCurSmbUrl)
        		    .setMessage("Number of total files : "+folderInfo.NumOfFiles+"\nTotal Size : " +(folderInfo.SizeOfFiles/(1024*1024)) + "MB")
        		    .setView(v)
        		    .setPositiveButton("NO", new DialogInterface.OnClickListener() 
        		    {
        		        @Override
        		        public void onClick(DialogInterface dialog, int which) {
        		        	Toast.makeText(getApplicationContext(), "Cancel Upload", Toast.LENGTH_SHORT).show();
        		        }
        		     })
        		     .setNegativeButton("YES", new DialogInterface.OnClickListener() 
         		    {
         		        @Override
         		        public void onClick(DialogInterface dialog, int which) {
         		        	
        	                progress.setTitle("Wait For Upload (Folder)");
        	                progress.setMessage("FROM	: " + mCurPath+"/"+mCurFileName +"\n\n"+"TO		: "+mCurSmbUrl+mCurFileName);
        	                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	                progress.setProgress(0);
        	                progress.setMax(mFolderSize);
        	                progress.show();
        	                
        	                SmbCreateFolder(mCurPath+"/"+mCurFileName);
        	                
                        	Thread thread = new Thread(new Runnable(){
        	                    @Override
        	                    public void run() {
        	                        try {
        	                        	mFilesUploaded = 0;
        	                        	SmbUploadFolder(mCurPath+"/"+mCurFileName);
        	                        	progress.dismiss();
        	                        } catch (Exception e) {
        	                            e.printStackTrace();
        	                        }
        	                    }
        	                });
        	                thread.start();
         		        }
         		     })
        		    .show();
            	}
            	else
            	{

	                progress.setTitle("Wait for upload");
	                progress.setMessage("FROM	: " + mCurPath+"/"+mCurFileName +"\n\n"+"TO		: "+mCurSmbUrl+mCurFileName);
	                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	                progress.setProgress(0);
	                progress.show();
	
	                Thread thread = new Thread(new Runnable(){
	                    @Override
	                    public void run() {
	                        try {
	                        	SmbUploadFile(mCurPath+"/",mCurFileName,false);
	                        	//copy("/mnt/sata/1080.mp4","nfs://192.168.0.185:/home/hank/nfs_share/1080.mp4");
	                        	progress.dismiss();
	                        } catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                    }
	                });
	                thread.start();
            	}
    		}
    	 }
    	else if(Option == 1)
    	{
    		if (resultCode == RESULT_OK)
    		{
    			
    			mCurFileName = data.getStringExtra("GetFileName"); 
    			mCurPath = data.getStringExtra("GetPath");
                progress.setTitle("Wait for upload");
                progress.setMessage("FROM	: " + mCurPath+"/"+mCurFileName +"\n\n"+"TO		: "+mCurNfsIP+":"+mCurNfsSharePath+"("+mCurNfsLocalPath+")");
                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progress.setProgress(0);
                progress.show();
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                        	CopyFileToNFS(mCurPath+"/",mCurFileName);
                        	progress.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
    		}
    	}
    }
}
