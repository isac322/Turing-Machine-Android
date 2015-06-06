package com.ph.turing.machine;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity implements FragmentTableRow.OnFragmentInteractionListener {
	private final String firstText = "상태 선택.";

	private final ArrayList<FragmentTableRow> fragments = new ArrayList<>();
	private final ArrayList<String[]> table = new ArrayList<>();
	private final ArrayList<String> states = new ArrayList<>();


	private Spinner startState, endState;
	ArrayAdapter<String> adapter;


	private static final String TAG = "adk";
	private static final String ACTION_USB_PERMISSION = "com.ph.turing.machine.action.USB_PERMISSION";
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// User가  Permission  메시지 박스에 응답했을 때
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					// User의  Permission을 수락했는지
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory " + accessory);
					}
					mPermissionRequestPending = false;
				}

			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				// Accessory 와  연결이 해제 되었을 때
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				// 현재  이 응용프로그램에 연결된 Accessory일 때 만
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		states.add(firstText);

		startState = (Spinner) findViewById(R.id.spin_start_state);
		endState = (Spinner) findViewById(R.id.spin_end_state);

		adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, states);

		startState.setAdapter(adapter);
		endState.setAdapter(adapter);


		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		// 연결된 Accessory 객체에 대해서  접근여부를 user에게 묻기 위해서 생성
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

		// ACTION_USB_PERMISSION , ACTION_USB_ACCESSORY_DETACHED  이벤트를 받기 위해서  등록
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
	}

	@Override
	protected void onResume() {
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		// Application 다시 시작 시  연결되어있는 Accessory가 있는 지 확인
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {   // 현재 Accessory가 하나만 연결되어있다는 가정하에 .
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {  // permission을 가지고  있지 않은 경우  User에게  permission 요청
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case R.id.add_row:
				addRow();
				break;

			case R.id.compile:
				checkTable();
				if (!send()) {
					Log.d(TAG, "fail send");
					Toast.makeText(getApplicationContext(), "fail to send", Toast.LENGTH_LONG).show();
				}
				Log.d(TAG, "success send");
				break;

			case R.id.delete_all:
				Log.d(TAG, "delete all table elements");

				FragmentTransaction transaction = getFragmentManager().beginTransaction();

				for (FragmentTableRow fragment : fragments) {
					transaction.remove(fragment);
				}

				states.clear();

				states.add(firstText);
				fragments.clear();

				transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
						.commit();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onFragmentInteraction(FragmentTableRow fragment) {
		int i = fragments.indexOf(fragment);

		states.remove(i);

		getFragmentManager()
				.beginTransaction()
				.remove(fragment)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
				.commit();
		fragments.remove(fragment);
	}

	@Override
	public void changStateName(FragmentTableRow fragment, String state_name) {
		int i = fragments.indexOf(fragment);
		states.set(i, state_name);

		adapter.notifyDataSetChanged();
	}

	private void addRow() {
		Log.d(TAG, "new fragments row");

		FragmentTableRow row = FragmentTableRow.newInstance();

		if (states.size() == 1 && states.get(0).equals(firstText)) {
			states.set(0, "");
		} else {
			states.add("");
		}

		fragments.add(row);

		getFragmentManager()
				.beginTransaction()
				.add(R.id.list_item, row)
				.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				.commit();
	}

	private void checkTable() {
		Log.d(TAG, "checking table");
		table.clear();

		for (FragmentTableRow row : fragments) {
			table.add(row.getElements());
		}
	}

	private boolean send() {
		Log.d(TAG, "attempt send table");

		ArrayList<Byte> result = new ArrayList<>();

		result.add((byte) table.size());
		result.add((byte) states.indexOf(startState.getSelectedItem().toString()));
		result.add((byte) states.indexOf(endState.getSelectedItem().toString()));

		for (String[] strings : table) {
			result.add((byte) states.indexOf(strings[0]));
			result.add(Byte.parseByte(strings[1]));
			result.add((byte) states.indexOf(strings[2]));
			result.add(Byte.parseByte(strings[3]));
			result.add(Byte.parseByte(strings[4]));

			result.add((byte) states.indexOf(strings[0]));
			result.add(Byte.parseByte(strings[5]));
			result.add((byte) states.indexOf(strings[6]));
			result.add(Byte.parseByte(strings[7]));
			result.add(Byte.parseByte(strings[8]));
		}

		String print = "";

		byte[] dataToSend = new byte[result.size()];

		for (int i = 0; i < result.size(); i++) {
			dataToSend[i] = result.get(i);

			if (i > 2) {
				print += String.valueOf(dataToSend[i]) + " ";
				if ((i - 2) % 5 == 0) {
					print += '\n';
				}
			} else {
				print += String.valueOf(dataToSend[i]) + '\n';
			}
		}

		Toast.makeText(getApplicationContext(), print, Toast.LENGTH_LONG).show();

		return sendTable(dataToSend);
	}

	public boolean sendTable(byte[] table) {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(table);

				return true;
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}

		return false;
	}

	private void openAccessory(UsbAccessory accessory) {
		// 안드로이드 시스템은 Accessory 디바이스와 연결되었을 때  Accessory에  데이터 송수신을 위해서
		// ParcelFileDescriptor를 제공한다.
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			// 데이터 송수신을 위해서 FileInputStream , FileOutputStream을 생성한다.
			// 이 예제에서는  데이터 전송만 쓰기 때문에  FileOutputStream 만을 사용.
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	// Accessory 로  전송을 위해서 생성된
	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException ignored) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}
}
