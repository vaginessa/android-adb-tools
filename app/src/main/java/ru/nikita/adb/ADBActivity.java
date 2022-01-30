package ru.nikita.adb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import ru.nikita.adb.Binary;
import ru.nikita.adb.Task;
import ru.nikita.adb.Device;
import ru.nikita.adb.AppListActivity;
import ru.nikita.adb.FileManagerActivity;
import ru.nikita.adb.AppManagerActivity;
import ru.nikita.adb.FastbootActivity;
import ru.nikita.adb.DeviceListAdapter;

public class ADBActivity extends Activity {
	private static final int APP_INSTALL_FILE=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adb_activity);
		
		text = (TextView)findViewById(R.id.log);

		adb = new Binary(getApplicationContext(), "adb");
		refreshDeviceList(null);
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.adb_activity, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id = item.getItemId();
		if(id == R.id.fastboot){
			Intent intent = new Intent(this, FastbootActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
	protected Spinner getDeviceList(){
		return (Spinner)findViewById(R.id.device);
	}
	protected void setDeviceList(DeviceListAdapter adapter){
		Spinner spinner = getDeviceList();
		spinner.setAdapter(adapter);
	}
	protected void clearDeviceList(){
		setDeviceList(null);
	}
	protected Device getSelectedDevice(){
		return (Device)getDeviceList().getSelectedItem();
	}
	public void refreshDeviceList(View view){
		new DeviceListTask(text,adb).execute();
	}
	private void disableEnableControls(boolean enable, ViewGroup vg){
		for (int i = 0; i < vg.getChildCount(); i++){
			View child = vg.getChildAt(i);
			child.setEnabled(enable);
			if (child instanceof ViewGroup)
				disableEnableControls(enable, (ViewGroup)child);
		}
	}
	public void connectDevice(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.ip);

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
		builder.setView(input);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String ip=input.getText().toString();
				new ADBTask(text,adb).connectDevice(ip);
				refreshDeviceList(null);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}
	public void disconnectAll(View view){
		new ADBTask(text,adb).execute("disconnect");
		refreshDeviceList(null);
	}
	public void startServer(View view){
		new ADBTask(text,adb).execute("start-server");
	}
	public void killServer(View view){
		new ADBTask(text,adb).execute("kill-server");
	}
	public void reconnect(View view){
		new ADBTask(text,adb).execute("reconnect");
		refreshDeviceList(null);
	}

	public void reboot(View view){
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.reboot);
		final String[] items = {
			"",
			"bootloader",
			"recovery",
			"sideload",
			"sideload-auto-reboot"
		};
		b.setItems(R.array.adb_reboot, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which){
				dialog.dismiss();
				new ADBTask(text,adb).reboot(getSelectedDevice(), items[which]);
			}
		});
		b.show();
	}
	public void installAppFromFile(View view){
		Intent chooseFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		chooseFileIntent.setType("application/vnd.android.package-archive");
		chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE);

		chooseFileIntent = Intent.createChooser(chooseFileIntent, getResources().getString(R.string.file_choose));
		startActivityForResult(chooseFileIntent, APP_INSTALL_FILE);
	}
	public void installAppFromList(View view){
		Intent intent = new Intent(this, AppListActivity.class);
		startActivityForResult(intent, APP_INSTALL_FILE);
	}
	public void fileManager(View view){
		Intent intent = new Intent(this, FileManagerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable("adb", adb);
		bundle.putSerializable("device", getSelectedDevice());
		intent.putExtras(bundle);
		startActivity(intent);
	}
	public void appManager(View view){
		Intent intent = new Intent(this, AppManagerActivity.class);
		Bundle bundle = new Bundle();
		bundle.putSerializable("adb", adb);
		bundle.putSerializable("device", getSelectedDevice());
		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && data != null){
			if(requestCode == APP_INSTALL_FILE){
				String filePath = data.getData().getPath();
				new ADBTask(text,adb).installAppFromFile(getSelectedDevice(),filePath);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	public void tcpip(View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.port);

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		input.setHint("5555");
		builder.setView(input);

		builder.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String port=input.getText().toString();
				if(port.trim().length()==0)port="5555";
				new ADBTask(text,adb).tcpip(getSelectedDevice(),port);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}
	public void executeCommand(View view){
		EditText command = (EditText)findViewById(R.id.command);
		new ADBTask(text,adb).execute(getSelectedDevice(), command.getText().toString());
	}
	private class DeviceListTask extends ADBTask{
		public DeviceListTask(TextView text, Binary binary){
			super(text, binary);
		}
		@Override
		protected void onPostExecute(String log){
			clearDeviceList();
			Device[] devices = getDeviceList(log);

			disableEnableControls(devices.length > 0, (ViewGroup)findViewById(R.id.controls));

			DeviceListAdapter adapter = new DeviceListAdapter(ADBActivity.this, devices);
			setDeviceList(adapter);
		}

		public void execute(){
			execute("devices");
		}
		private Device[] getDeviceList(String log){
			ArrayList<Device> devices = new ArrayList<Device>();
			String lines[] = log.split("\\n");
			Pattern pattern = Pattern.compile("^(\\S+)\\s+(\\S+)");
			Matcher matcher;
			for(String line : lines){
				if (line.matches(pattern.pattern())) {
					matcher = pattern.matcher(line);
					if (matcher.find())
						devices.add(new Device(matcher.group(1),matcher.group(2)));
				}
			}
			return devices.toArray(new Device[0]);
		}
	}

	

	private TextView text;
	private Binary adb;

}
