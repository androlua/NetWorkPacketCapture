/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.minhui.networkcapture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.minhui.vpn.LocalVPNService;
import com.minhui.vpn.VPNConnectManager;

import java.util.ArrayList;


public class VPNCaptureActivity extends FragmentActivity {
    private static final int VPN_REQUEST_CODE = 101;
    private static final int REQUEST_PACKAGE = 103;
    private static final String DATA_SAVE = "saveData";
    private static final String DEFAULT_PACKAGE_ID = "default_package_id";
    private static final String DEFAULT_PACAGE_NAME = "default_package_name";
    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            vpnButton.setImageResource(LocalVPNService.isRunning() ? R.mipmap.ic_stop : R.mipmap.ic_start);
        }
    };
    private ImageView vpnButton;
    private TextView packageId;
    private SharedPreferences sharedPreferences;
    private String selectPackage;
    private String selectName;
    private ArrayList<BaseFragment> baseFragments;
    private TabLayout tabLayout;
    private FragmentPagerAdapter simpleFragmentAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpn_capture);
        vpnButton = (ImageView) findViewById(R.id.vpn);
        vpnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LocalVPNService.isRunning()) {
                    closeVpn();
                } else {
                    startVPN();
                }
            }
        });
        packageId = (TextView) findViewById(R.id.package_id);

        sharedPreferences = getSharedPreferences(DATA_SAVE, MODE_PRIVATE);
        selectPackage = sharedPreferences.getString(DEFAULT_PACKAGE_ID, null);
        selectName = sharedPreferences.getString(DEFAULT_PACAGE_NAME, null);
        packageId.setText(selectName != null ? selectName :
                selectPackage != null ? selectPackage : getString(R.string.all));
        vpnButton.setEnabled(true);
        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalVPNService.BROADCAST_VPN_STATE));
        //  summerState = findViewById(R.id.summer_state);
        findViewById(R.id.select_package).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VPNCaptureActivity.this, PackageListActivity.class);
                startActivityForResult(intent, REQUEST_PACKAGE);
            }
        });

        initChildFragment();
        initViewPager();
        initTab();
    }

    private void initViewPager() {
        simpleFragmentAdapter=new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return baseFragments.get(position);
            }

            @Override
            public int getCount() {
                return baseFragments.size();
            }
        };
   //     simpleFragmentAdapter = new SimpleFragmentAdapter(baseFragments, getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.container_vp);
        viewPager.setAdapter(simpleFragmentAdapter);
    }

    private void initTab() {
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        String[] tabTitle = getResources().getStringArray(R.array.tabs);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            tab.setText(tabTitle[i]);
        }
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
             //   viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    private void initChildFragment() {
        baseFragments = new ArrayList<>();
     // BaseFragment captureFragment = new SimpleFragment();
           BaseFragment captureFragment = new CaptureFragment();
        BaseFragment historyFragment = new HistoryFragment();
        BaseFragment settingFragment = new SettingFragment();
        baseFragments.add(captureFragment);
        baseFragments.add(historyFragment);
        baseFragments.add(settingFragment);

    }

    private void closeVpn() {
        Intent intent = new Intent(this, LocalVPNService.class);
        intent.setAction(LocalVPNService.ACTION_CLOSE_VPN);
        startService(intent);

    }

    private void startVPN() {

        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vpnStateReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            Intent intent = new Intent(this, LocalVPNService.class);
            intent.setAction(LocalVPNService.ACTION_START_VPN);
            if (selectPackage != null) {
                intent.putExtra(LocalVPNService.SELECT_PACKAGE_ID, selectPackage.trim());
            }
            startService(intent);

            VPNConnectManager.getInstance().resetNum();
        } else if (requestCode == REQUEST_PACKAGE && resultCode == RESULT_OK) {
            PackageShowInfo showInfo = (PackageShowInfo) data.getParcelableExtra(PackageListActivity.SELECT_PACKAGE);
            if (showInfo == null) {
                selectPackage = null;
                selectName = null;
            } else {
                selectPackage = showInfo.packageName;
                selectName = showInfo.appName;
            }
            packageId.setText(selectName != null ? selectName :
                    selectPackage != null ? selectPackage : getString(R.string.all));
            vpnButton.setEnabled(true);
            sharedPreferences.edit().putString(DEFAULT_PACKAGE_ID, selectPackage)
                    .putString(DEFAULT_PACAGE_NAME, selectName).apply();
        }
    }


}