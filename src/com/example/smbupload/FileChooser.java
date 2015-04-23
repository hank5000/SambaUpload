package com.example.smbupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.sql.Date;
import java.util.ArrayList; 
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.DateFormat; 

import com.example.smbupload.R;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import android.os.Bundle; 
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent; 
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView; 
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class FileChooser extends Activity {
	private static final int MENU_FILTER_ENABLE = Menu.FIRST,
							 MENU_FILTER_DISABLE = Menu.FIRST+1;
	int choose_year=0;
	int choose_month=0;
	int choose_date=0;
	int choose_other = 0;
	boolean bNeedFilter = false;
	private File currentDir;
    private FileArrayAdapter adapter;
    private GridView gridView;
    String mHomeDir = "";
    
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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String HomeDir = "";
        HomeDir = readFileData("HOMEDIR");
        
        if(HomeDir.length()==0)
        {
        	HomeDir = "/mnt/sata/";
        }
        mHomeDir = HomeDir;
        currentDir = new File(HomeDir);
        setContentView(R.layout.grid_view);

        fill(currentDir);
    }
    
    public String getExtension(File file)
    {
        int startIndex = file.getName().lastIndexOf(46) + 1;
        int endIndex = file.getName().length();
        return  file.getName().substring(startIndex, endIndex);
    }

    private void fill(File f)
    {
    	File[]dirs = f.listFiles(); 
        this.setTitle("Dir: "+ f.getAbsolutePath());

    	if(bNeedFilter)
    	{
    		this.setTitle("Filter"+choose_year+"/"+choose_month+"/"+choose_date);
    	}
    	else
    	{
    		//this.setTitle("All File");
    	}
		 List<Item>dir = new ArrayList<Item>();
		 List<Item>fls = new ArrayList<Item>();
		 
		 try{
			 for(File ff: dirs)
			 { 
				boolean bNeedToAdd = false;
				Date lastModDate = new Date(ff.lastModified()); 
				DateFormat formater = DateFormat.getDateTimeInstance();
				String date_modify = formater.format(lastModDate);
				String file_name   = ff.getName();
				if(ff.isDirectory()){

					File[] fbuf = ff.listFiles(); 
					int buf = 0;
					if(fbuf != null){ 
						buf = fbuf.length;
					} 
					else buf = 0; 
					String num_item = String.valueOf(buf);
					if(buf == 0) num_item = num_item + " item";
					else num_item = num_item + " items";

					//String formated = lastModDate.toString();
					if(ff.getPath().equals("/mnt/nfs"))
					{
						dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"directory_nfs_icon")); 
					}
					else
					{
						dir.add(new Item(ff.getName(),num_item,date_modify,ff.getAbsolutePath(),"directory_icon")); 
					}
				}
				else
				{
					String file_type = getExtension(ff);
					String file_icon = "file_";

					
					if(file_type.compareTo("mp4")==0|
					   file_type.compareTo("avi")==0|
					   file_type.compareTo("mkv")==0|
					   file_type.compareTo("mov")==0)
					{
						file_icon = file_icon + file_type;
					}
					else
					{
						file_icon = file_icon + "icon";
					}
					if(bNeedFilter)
					{
						//if((file_year==choose_year) && (file_month==choose_month ) && (file_date==choose_date))

							fls.add(new Item(ff.getName(),ff.length() + " Byte", date_modify, ff.getAbsolutePath(),file_icon));
					}
					else
					{
						fls.add(new Item(ff.getName(),ff.length() + " Byte", date_modify, ff.getAbsolutePath(),file_icon));
					}
				}
			 }
		 }catch(Exception e)
		 {    
			 Toast.makeText(getApplicationContext(), "Something wrong!!!", Toast.LENGTH_SHORT).show();
		 }
		 Collections.sort(dir);
		 Collections.sort(fls);
		 dir.addAll(fls);

         //if(!f.getName().equalsIgnoreCase("sata"))
         dir.add(0,new Item("..","Parent Directory","",f.getParent(),"directory_up"));

		 //setContentView(R.id.main_page_gridview);
		 gridView = (GridView)findViewById(R.id.main_page_gridview);
		 adapter = new FileArrayAdapter(FileChooser.this,R.layout.grid_item,dir);
		 gridView.setAdapter(adapter);
		 gridView.setOnItemClickListener(mListener);
		 gridView.setOnItemLongClickListener(mListenerLong);
    }
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, MENU_FILTER_ENABLE, 0, "Filter File Enable");
		menu.add(0, MENU_FILTER_DISABLE, 0, "Filter File Disable");

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub

		switch (item.getItemId()) {
		case MENU_FILTER_ENABLE:
			LayoutInflater inflater = (LayoutInflater) FileChooser.this.getSystemService(LAYOUT_INFLATER_SERVICE);
			final View v = inflater.inflate(R.layout.smbserverinfo,null);

			new AlertDialog.Builder(FileChooser.this)
		    .setTitle("Filter Setting")
		    .setView(v)
		    .setPositiveButton("Enter", new DialogInterface.OnClickListener() 
		    {
		        @Override
		        public void onClick(DialogInterface dialog, int which) {                               
				EditText editText_year = (EditText) (v.findViewById(R.id.edittext_year));
				EditText editText_month = (EditText) (v.findViewById(R.id.edittext_month));
				EditText editText_date = (EditText) (v.findViewById(R.id.edittext_date));
				EditText editText_other = (EditText) (v.findViewById(R.id.edittext_other));
				
				if("".equals(editText_year.getText().toString().trim()) || "".equals(editText_month.getText().toString().trim()) || "".equals(editText_date.getText().toString().trim()))
				{
					Toast.makeText(getApplicationContext(), "Invalid Value ( Value = NULL )", Toast.LENGTH_SHORT).show();
				}
				else
				{
					choose_year 	= Integer.valueOf(editText_year.getText().toString());
					choose_month	= Integer.valueOf(editText_month.getText().toString());
					choose_date		= Integer.valueOf(editText_date.getText().toString());
					choose_other	= Integer.valueOf(editText_other.getText().toString());
					Toast.makeText(getApplicationContext(), "input year:"+editText_year.getText().toString()+" month: "+editText_month.getText().toString()+" date: "+editText_date.getText().toString(), Toast.LENGTH_SHORT).show();
				}
	
				bNeedFilter = true;
				fill(currentDir);
		    }
		    })
		    .show();

			break;
		case MENU_FILTER_DISABLE:
			bNeedFilter = false;
			fill(currentDir);
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	protected GridView.OnItemLongClickListener mListenerLong = new GridView.OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			Item o = adapter.getItem(position);

			onFileClick(o);

			return true;
		}};
	
	protected GridView.OnItemClickListener mListener = new GridView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			Item o = adapter.getItem(position);
			if(o.getImage().equalsIgnoreCase("directory_icon")||o.getImage().equalsIgnoreCase("directory_up")){
					currentDir = new File(o.getPath());
					fill(currentDir);
			}
			else
			{
				onFileClick(o);
			}
		}};
	

    private void onFileClick(Item o)
    {
    	//Toast.makeText(this, "Folder Clicked: "+ currentDir, Toast.LENGTH_SHORT).show();
    	Intent intent = new Intent();
        intent.putExtra("GetPath",currentDir.toString());
        intent.putExtra("GetFileName",o.getName());
        setResult(RESULT_OK, intent);
        finish();
    }
}
