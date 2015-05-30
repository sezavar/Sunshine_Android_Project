package org.sezavar.android.sunshine.app;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailedActivityFragment extends Fragment {

    public DetailedActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView= inflater.inflate(R.layout.fragment_detailed, container, false);
        TextView forecastText=(TextView)rootView.findViewById(R.id.forcast_text);
        forecastText.setText(getActivity().getIntent().getExtras().getString(Intent.EXTRA_TEXT));
        return  rootView;
    }
}
