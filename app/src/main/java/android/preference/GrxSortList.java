/*      2017 Grouxho (esp-desarrolladores.com)

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/

package android.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.mods.grx.settings.GrxPreferenceScreen;
import com.mods.grx.settings.R;
import com.mods.grx.settings.dlgs.DlgFrGrxSortList;
import com.mods.grx.settings.Common;
import com.mods.grx.settings.prefssupport.info.PrefAttrsInfo;
import com.mods.grx.settings.utils.Utils;


public class GrxSortList extends Preference implements DlgFrGrxSortList.OnGrxOrdenarListaListener,
        GrxPreferenceScreen.CustomDependencyListener   {

    private boolean mSortIcon;
    private String mPrefValue;
    private String mDefValue;
    String values_array_name;
    private int id_options_array;
    private int id_values_array;
    private int id_icons_array;
    private Runnable RDobleClick;
    private Handler handler;
    private boolean doble_clic_pendiente;
    private Long timeout;
    private int clicks;

    private ImageView vWidgetArrow;
    private ImageView vAndroidIcon;

    private PrefAttrsInfo myPrefAttrsInfo;

    public GrxSortList(Context c){
        super(c);
    }

    public GrxSortList(Context c, AttributeSet a){
        super(c,a);
        ini_param(c,a);
    }


    public GrxSortList(Context context, AttributeSet attrs, int defStyleAttr){
        super(context,attrs,defStyleAttr);
        ini_param(context,attrs);
    }


    private void ini_param(Context c, AttributeSet attrs) {
        clicks=0;
        myPrefAttrsInfo = new PrefAttrsInfo(c, attrs, getTitle(), getSummary(),getKey(), true);

        mSortIcon = attrs.getAttributeBooleanValue(null, "grxSortIcon", getContext().getResources().getBoolean(R.bool.def_Show_SortIcon));
        id_options_array = array_id(attrs.getAttributeValue(null,"grxA_entries"));
        id_values_array = array_id(attrs.getAttributeValue(null,"grxA_values"));
        id_icons_array = array_id(attrs.getAttributeValue(null,"grxA_ics"));


        values_array_name=attrs.getAttributeValue(null,"grxA_values");
        mDefValue = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue");

        if(mDefValue==null || mDefValue.isEmpty()){
            mDefValue= Utils.get_formatted_string_from_array_res(getContext(),id_values_array,myPrefAttrsInfo.get_my_separator());
          }

        //si no existe android:defaultValue -> establecemos un valor por defecto y así se ejecuta onSetinitial y podemos persistir y sincronizar con settings system
        //importante no hacer nada en ongetdefaultvalue, pues si en el xml existe android:defaultValue, será el primer método que se ejecute, antes que ini_param y no
        //estará inicializado nada de nada.

        setDefaultValue(mDefValue);
        setSummary(myPrefAttrsInfo.get_my_summary());
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        vWidgetArrow = (ImageView) view.findViewById(R.id.widget_arrow);
        vAndroidIcon = (ImageView) view.findViewById(android.R.id.icon);
        vAndroidIcon.setLayoutParams(Common.AndroidIconParams);
        return view;
    }

    @Override
    public void onBindView(View view) {
        float alpha = (isEnabled() ? (float) 1.0 : (float) 0.4);
        if (vWidgetArrow != null) vWidgetArrow.setAlpha(alpha);
        if (vAndroidIcon != null) vAndroidIcon.setAlpha(alpha);
        super.onBindView(view);
    }



    private void set_up_double_click(){
        handler = new Handler();
        timeout = Long.valueOf(ViewConfiguration.getDoubleTapTimeout());
        doble_clic_pendiente=false;

        RDobleClick = new Runnable() {
            @Override
            public void run() {
                if(!doble_clic_pendiente){
                    accion_click();
                }else {
                    if(clicks==0) accion_click();
                    else if(clicks!=2) {
                        accion_click();
                    }else {
                        accion_doble_click();
                    }
                }
            }
        };
    }



    @Override
    protected void onClick() {
        if(handler==null) set_up_double_click();
        if(RDobleClick==null) show_dialog();
        else{
            clicks++;
            if(!doble_clic_pendiente){
                handler.removeCallbacks(RDobleClick);
                doble_clic_pendiente=true;
                handler.postDelayed(RDobleClick,timeout);
            }
        }
    }

    private void accion_doble_click(){
        clicks=0;
        doble_clic_pendiente=false;
        handler.removeCallbacks(RDobleClick);
        show_reset_dialog();
    }

    private void show_reset_dialog(){
        if(mPrefValue.isEmpty()) return;
        AlertDialog dlg = new AlertDialog.Builder(getContext()).create();
        dlg.setTitle(getContext().getResources().getString(R.string.gs_tit_reset));
        dlg.setMessage(getContext().getResources().getString(R.string.gs_mensaje_reset));
        dlg.setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(R.string.gs_si), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefValue= mDefValue;
                save_value();
            }
        });
        dlg.show();
    }

    private void accion_click(){
        clicks=0;
        doble_clic_pendiente=false;
        handler.removeCallbacks(RDobleClick);
        show_dialog();
    }

    private int array_id(String array_name){
        if(array_name!=null && !array_name.isEmpty()){
            String aux[]=array_name.split("/");
            return getContext().getResources().getIdentifier(aux[1], "array", getContext().getPackageName() );
        }else return 0;
    }

    private void show_dialog(){

        GrxPreferenceScreen grxPreferenceScreen = (GrxPreferenceScreen) getOnPreferenceChangeListener();
        if(grxPreferenceScreen !=null){
            DlgFrGrxSortList dlg = (DlgFrGrxSortList) grxPreferenceScreen.getFragmentManager().findFragmentByTag("DlgFrGrxSortList");
            if(dlg==null){
                dlg = DlgFrGrxSortList.newInstance(this, Common.TAG_PREFSSCREEN_FRAGMENT, getKey(), getTitle().toString(), mPrefValue, myPrefAttrsInfo.get_my_separator(),
                        id_options_array, id_values_array, id_icons_array,mSortIcon);
                dlg.show(grxPreferenceScreen.getFragmentManager(),"DlgFrGrxSortList");
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        mDefValue = a.getString(index);
        return mDefValue;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mPrefValue=getPersistedString(mDefValue);
        } else {
            mPrefValue=mDefValue;
            persistString(mPrefValue);
        }
        save_value_in_settings(mPrefValue);
    }



    private void save_value_in_settings(String valor){
            if(mPrefValue.equals(valor)) return;
            if(!myPrefAttrsInfo.is_valid_key()) return;
            if(myPrefAttrsInfo.get_allowed_save_in_settings_db()){
                String real = Settings.System.getString(getContext().getContentResolver(), this.getKey());
                if(real==null) real="N/A";
                if (!real.equals(mPrefValue)) {
                    Settings.System.putString(getContext().getContentResolver(), this.getKey(), mPrefValue);
                }
            }
    }


    private void save_value() {
        persistString(mPrefValue);
        notifyChanged();
        save_value_in_settings(mPrefValue);
        if(getOnPreferenceChangeListener()!=null) getOnPreferenceChangeListener().onPreferenceChange(this,mPrefValue);
        send_broadcasts_and_change_group_key();
    }


    @Override
    public void onGrxOrdenarLista(String valor){
        if(!mPrefValue.equals(valor)) {
            mPrefValue=valor;
            save_value();
        }
    }


    /********* broadcast , change group key value **********/

    private void send_broadcasts_and_change_group_key(){
        if(Common.SyncUpMode) return;
        GrxPreferenceScreen chl = (GrxPreferenceScreen) getOnPreferenceChangeListener();
        if(chl!=null){
            chl.send_broadcasts_and_change_group_key(myPrefAttrsInfo.get_my_group_key(),myPrefAttrsInfo.get_send_bc1(),myPrefAttrsInfo.get_send_bc2());
        }
    }

    /**********  Onpreferencechangelistener - add custom dependency rule *********/

    @Override
    public void setOnPreferenceChangeListener(Preference.OnPreferenceChangeListener onPreferenceChangeListener){
        if(!Common.SyncUpMode){
            super.setOnPreferenceChangeListener(onPreferenceChangeListener);
            String mydeprule = myPrefAttrsInfo.get_my_dependency_rule();
            if(mydeprule!=null){
                GrxPreferenceScreen grxPreferenceScreen = (GrxPreferenceScreen) getOnPreferenceChangeListener();
                grxPreferenceScreen.add_custom_dependency(this,mydeprule,null);
            }

        }else {
            GrxPreferenceScreen grxPreferenceScreen = (GrxPreferenceScreen) onPreferenceChangeListener;
            grxPreferenceScreen.add_group_key_for_syncup(myPrefAttrsInfo.get_my_group_key());
        }

    }

    /************ custom dependencies ****************/
    public void OnCustomDependencyChange(boolean state){
        setEnabled(state);
    }


}