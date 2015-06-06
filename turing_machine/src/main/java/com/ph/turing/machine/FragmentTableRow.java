package com.ph.turing.machine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentTableRow.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentTableRow#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentTableRow extends Fragment {
	private OnFragmentInteractionListener mListener;

	private EditText currentState, nextState, nextState2;
	private Switch dataToSave, dataToSave2, nextDirection, nextDirection2;
	private ImageButton btn_del;

	private AlertDialog.Builder builder;


	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment FragmentTableRow.
	 */
	public static FragmentTableRow newInstance() {
		return new FragmentTableRow();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment

		View v = inflater.inflate(R.layout.fragment_table_row, container, false);

		builder = new AlertDialog.Builder(getActivity())
				.setTitle("상태 삭제")
				.setMessage("선택한 행을 삭제하시겠습니까?")
				.setCancelable(false)
				.setPositiveButton("화인", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						onButtonPressed(FragmentTableRow.this);
					}
				})
				.setNegativeButton("취소", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
					}
				});

		currentState = (EditText) v.findViewById(R.id.edit_current_state);
		nextState = (EditText) v.findViewById(R.id.edit_next);
		nextState2 = (EditText) v.findViewById(R.id.edit_next2);
		dataToSave = (Switch) v.findViewById(R.id.switch_data_save);
		dataToSave2 = (Switch) v.findViewById(R.id.switch_data_save2);
		nextDirection = (Switch) v.findViewById(R.id.switch_next_direction);
		nextDirection2 = (Switch) v.findViewById(R.id.switch_next_direction2);
		btn_del = (ImageButton) v.findViewById(R.id.delete_row);


		currentState.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				if (!b) {
					mListener.changStateName(FragmentTableRow.this, currentState.getText().toString());
				}
			}
		});

		btn_del.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				builder.create().show();
			}
		});

		return v;
	}

	public void onButtonPressed(FragmentTableRow fragment) {
		if (mListener != null) {
			mListener.onFragmentInteraction(fragment);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		void onFragmentInteraction(FragmentTableRow fragment);
		void changStateName(FragmentTableRow fragment, String state_name);
	}

	public String[] getElements() {
		return new String[]{
				currentState.getText().toString(),
				"0",
				nextState.getText().toString(),
				dataToSave.isChecked() ? "1" : "0",
				nextDirection.isChecked() ? "1" : "0",
				"1",
				nextState2.getText().toString(),
				dataToSave2.isChecked() ? "1" : "0",
				nextDirection2.isChecked() ? "1" : "0",
		};
	}

	public String getStateName() {
		return currentState.getText().toString();
	}
}
