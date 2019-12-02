package sst.example.androiddemo.feature.activity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import sst.example.androiddemo.feature.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NormalFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NormalFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NormalFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static  final  String TAG = "NormalFragment";
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public NormalFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NormalFragment.
     */
    public static NormalFragment newInstance(String param1, String param2) {
        NormalFragment fragment = new NormalFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        super.onAttachFragment(childFragment);
        Log.d(TAG," onAttachFragment ==== ");

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG," onAttach ==== ");

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG," onActivityCreated ==== ");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Log.d(TAG," onCreate ==== ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG," onCreateView ==== ");

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_normal, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG," onStart ==== ");

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG," onResume ==== ");

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG," onPause ==== ");

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG," onStop ==== ");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG," onDestroy ==== ");

    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
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
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
