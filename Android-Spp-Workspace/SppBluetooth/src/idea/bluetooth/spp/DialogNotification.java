package idea.bluetooth.spp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Dialog
 * @author cxphong
 */
@SuppressLint("NewApi") public class DialogNotification extends DialogFragment{
	private String mMessage = null;
	private String mTitle = null;
	
	public static DialogNotification newInstance(String message, String title){
		DialogNotification f = new DialogNotification();

		Bundle args = new Bundle();
		args.putString("MESSAGE", message);
		args.putString("TITLE", title);
		f.setArguments(args);

		return f;
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) 
    {
    	mMessage = getArguments().getString("MESSAGE");
    	mTitle = getArguments().getString("TITLE");
    	
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mMessage);
        builder.setTitle(mTitle);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
               
        return builder.create();
    }
}

