package com.example.smbupload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

import android.widget.Toast;


public class Property {
	Class SystemProperties = null;
	Method GET = null;
	Method SET = null;
	static String prefix = "debug";
	static String IP_ADDR = prefix+".nfs.ip_address";
	static String SHARE_PATH = prefix+".nfs.share_folder_path";
	static String LOCAL_PATH = prefix+".nfs.mount_path";
	static String NEED_MOUNT = prefix+".nfs.need_mount";
	static String NEED_UMOUNT= prefix+".nfs.need_umount";
	static String FLUSH = prefix+".nfs.flush";
	static String UMOUNT_PATH = prefix+".nfs.umount_path";
	String[] RecNFSMount = null;
	int NumOfNFSMount = 0;

	public boolean Init()
	{
		try {
			SystemProperties = Class.forName("android.os.SystemProperties");
	        Class[] paramTypes = new Class[1];
	        paramTypes[0] = String.class;
	        // get method first
			GET = SystemProperties.getMethod("get",paramTypes);
			SET = SystemProperties.getMethod("set",new Class[] {String.class, String.class});
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if( GET == null | SET == null)
		{
			return false;
		}
		return true;
	}

	public String GetProp(String prop) 
	{
		String ret = "FAIL";
		if(GET!=null)
		{
	        try {
				ret = (String) GET.invoke(SystemProperties, prop);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return ret;
	}

	public boolean SetProp(String prop,String value) 
	{
		boolean ret = false;

		if(SET!=null)
		{
	        try {
				SET.invoke(SystemProperties, new Object[] {prop, value});
				ret = true;
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	public void NFSMount(String ip_address, String share_folder_path, String local_path)
	{
		SetProp(IP_ADDR,	ip_address);
		SetProp(SHARE_PATH,	share_folder_path);
		SetProp(LOCAL_PATH,	local_path);
		SetProp(NEED_MOUNT,	"1");
		SetProp(FLUSH,		"1");
	}
	
	
	// Chekc Mount can use without init
	public boolean CheckMount(String ip_address, String share_folder_path, String local_path)
	{
		boolean ret = false;
		// Scanner usb-disk 1/2/3/4 or none.
		//File mountFile = new File("/proc/mounts");
		try {
			FileReader fr = new FileReader("/proc/mounts");
			BufferedReader bf = new BufferedReader(fr);
			String line;
			try {
				while((line = bf.readLine())!=null)
				{
					String checkSentence=ip_address+":"+share_folder_path+" "+local_path;
					if(line.length()>checkSentence.length())
					{
						String SubSequ = (String) line.subSequence(0, checkSentence.length());
						if(SubSequ.equals(checkSentence))
						{
							ret = true;
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	
	public void UnMount(String local_path)
	{
		boolean CanUmount = true;
//		int i;
//		for(i=0;i<NumOfNFSMount;i++)
//		{
//			if(RecNFSMount[i].equals(local_path))
//			{
//				CanUmount = true;
//			}
//		}
		if(CanUmount)
		{
			SetProp(UMOUNT_PATH,	local_path);
			SetProp(NEED_UMOUNT,	"1");
			SetProp(FLUSH,			"1");
//			for(int j=i;j<NumOfNFSMount;j++)
//			{
//				RecNFSMount[j] = RecNFSMount[j+1];
//			}
//			NumOfNFSMount = NumOfNFSMount-1;
		}
	}
}
