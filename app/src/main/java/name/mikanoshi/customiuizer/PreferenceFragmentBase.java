package name.mikanoshi.customiuizer;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import miui.preference.PreferenceFragment;
import name.mikanoshi.customiuizer.utils.Helpers;

public class PreferenceFragmentBase extends PreferenceFragment {

	public boolean isAnimating = false;
	public int animDur = 350;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Activity act = getActivity();
		if (item.getItemId() == android.R.id.home) {
			if (this instanceof MainFragment)
				act.finish();
			else
				((SubFragment)this).finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initFragment() {
		setHasOptionsMenu(true);

		boolean showBack = false;
		if (this instanceof MainFragment) {
			ActivityInfo appInfo;
			try {
				appInfo = getActivity().getPackageManager().getActivityInfo(getActivity().getComponentName(), PackageManager.GET_META_DATA);
				showBack = appInfo != null && appInfo.metaData != null && appInfo.metaData.containsKey("from.settings");
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		} else showBack = true;

		getActionBar().setTitle(R.string.app_name);
		getActionBar().setDisplayHomeAsUpEnabled(showBack);
	}

	public void onCreate(Bundle savedInstanceState, int pref_defaults) {
		super.onCreate(savedInstanceState);
		try {
			getPreferenceManager().setSharedPreferencesName(Helpers.prefsName);
			getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
			getPreferenceManager().setStorageDeviceProtected();
			PreferenceManager.setDefaultValues(getActivity(), pref_defaults, false);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initFragment();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setViewBackground(view);
	}

	public void setViewBackground(View view) {
		if (Helpers.isPiePlus())
			view.setBackgroundResource(getResources().getIdentifier(Helpers.isNightMode(getContext()) ? "settings_window_bg_dark" : "settings_window_bg_light", "drawable", "miui"));
		else
			view.setBackgroundColor(Helpers.isNightMode(getContext()) ? Color.BLACK : Color.rgb(247, 247, 247));
	}

	public void openSubFragment(Fragment fragment, Bundle args, Helpers.SettingsType settingsType, Helpers.ActionBarType abType, int titleResId, int contentResId) {
		if (args == null) args = new Bundle();
		args.putInt("settingsType", settingsType.ordinal());
		args.putInt("abType", abType.ordinal());
		args.putInt("titleResId", titleResId);
		args.putInt("contentResId", contentResId);
		float order = 100.0f;
		try {
			if (getView() != null) order = getView().getTranslationZ();
		} catch (Throwable t) {}
		args.putFloat("order", order);
		if (fragment.getArguments() == null)
			fragment.setArguments(args);
		else
			fragment.getArguments().putAll(args);
		getFragmentManager().beginTransaction().setCustomAnimations(R.animator.fragment_open_enter, R.animator.fragment_open_exit, R.animator.fragment_close_enter, R.animator.fragment_close_exit)
			.replace(R.id.fragment_container, fragment).addToBackStack(null).commitAllowingStateLoss();
		getFragmentManager().executePendingTransactions();
	}

	@Override
	public Animator onCreateAnimator(int transit, boolean enter, final int nextAnim) {
		if (nextAnim == 0) return null;
		Configuration config = getResources().getConfiguration();
		float density = getResources().getDisplayMetrics().density;
		final float scrWidth = config.screenWidthDp * density;

		final View top = getView();
		if (top == null) return null;
		final View content = top.findViewById(android.R.id.content);

		//ValueAnimator.setFrameDelay(17);
		ValueAnimator valAnimator = new ValueAnimator();
		valAnimator.setDuration(animDur);
		valAnimator.setFloatValues(0.0f, 1.0f);

		if (nextAnim == R.animator.fragment_open_enter || nextAnim == R.animator.fragment_open_exit)
		valAnimator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
//				Log.e("animation", "start on: " + PreferenceFragmentBase.this.getClass().getCanonicalName());
				isAnimating = true;
			}

			@Override
			public void onAnimationEnd(Animator animation) {
//				Log.e("animation", "end on: " + PreferenceFragmentBase.this.getClass().getCanonicalName());
				isAnimating = false;
			}

			@Override
			public void onAnimationCancel(Animator animation) {
//				Log.e("animation", "cancel");
				isAnimating = false;
			}

			@Override
			public void onAnimationRepeat(Animator animation) {}
		}); else isAnimating = false;

		valAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (content == null) return;
				float val = (float)animation.getAnimatedValue();
				if (nextAnim == R.animator.fragment_open_enter) {
					top.setX(scrWidth * (1.0f - val));
					content.setAlpha(0.6f + val * 0.4f);
				} else if (nextAnim == R.animator.fragment_open_exit) {
					top.setX(-scrWidth / 4.0f * val);
					top.setAlpha(1.0f - val * 0.4f);
				} else if (nextAnim == R.animator.fragment_close_enter) {
					top.setX(-scrWidth / 4.0f * (1.0f - val));
					top.setAlpha(0.6f + val * 0.4f);
				} else if (nextAnim == R.animator.fragment_close_exit) {
					top.setX(scrWidth * val);
					content.setAlpha(1.0f - val * 0.4f);
				}
			}
		});

		return valAnimator;
	}
}