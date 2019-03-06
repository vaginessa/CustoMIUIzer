package name.mikanoshi.customiuizer;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import miui.app.ActionBar;
import miui.app.AlertDialog;

import name.mikanoshi.customiuizer.R;
import name.mikanoshi.customiuizer.subs.Launcher;
import name.mikanoshi.customiuizer.utils.Helpers;

public class MainFragment extends PreferenceFragmentBase {

	public boolean miuizerModuleActive = false;

	public MainFragment() {
		super();
		this.setRetainInstance(true);
	}

	private Runnable showUpdateNotification = new Runnable() {
		@Override
		public void run() {
			if (isFragmentReady(getActivity())) try {
				TextView update = (TextView)getActivity().findViewById(R.id.update);
				update.setText(Helpers.l10n(getActivity(), R.string.update_available));
				update.setTextColor(getResources().getColor(android.R.color.background_light));

				FrameLayout updateFrame = (FrameLayout)getActivity().findViewById(R.id.updateFrame);
				updateFrame.setLayoutTransition(new LayoutTransition());
				updateFrame.setVisibility(View.VISIBLE);
				updateFrame.setBackgroundColor(0xff252525);
				updateFrame.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							Intent detailsIntent = new Intent();
							detailsIntent.setComponent(new ComponentName("de.robv.android.xposed.installer", "de.robv.android.xposed.installer.DownloadDetailsActivity"));
							detailsIntent.setData(Uri.fromParts("package", "com.sensetoolbox.six", null));
							startActivity(detailsIntent);
						} catch (Exception e) {
							Helpers.openURL(getActivity(), "http://sensetoolbox.com/6/download");
						}
					}
				});
			} catch (Exception e) {}
		}
	};

	private Runnable hideUpdateNotification = new Runnable() {
		@Override
		public void run() {
			if (isFragmentReady(getActivity())) try {
				FrameLayout updateFrame = getActivity().findViewById(R.id.updateFrame);
				updateFrame.setVisibility(View.GONE);
			} catch (Exception e) {}
		}
	};

	private boolean isFragmentReady(Activity act) {
		return act != null && !act.isFinishing() && MainFragment.this.isAdded();
	}

	@Override
	@SuppressLint("MissingSuperCall")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.xml.preferences);
		addPreferencesFromResource(R.xml.preferences);
	}

	private void setupImmersiveMenu() {
		ActionBar actionBar = getActionBar();
		if (actionBar != null) actionBar.showSplitActionBar(false, false);
		setImmersionMenuEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		setupImmersiveMenu();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupImmersiveMenu();
		final Activity act = getActivity();
		final Handler handler = new Handler();

		// Preventing launch delay
		new Thread(new Runnable() {
			public void run() {
				if (!Helpers.isXposedInstallerInstalled(act))
				act.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(act);
						builder.setTitle(Helpers.l10n(act, R.string.xposed_not_found));
						builder.setMessage(Helpers.l10n(act, R.string.xposed_not_found_explain));
						builder.setNeutralButton(Helpers.l10n(act, R.string.okay), null);
						AlertDialog dlg = builder.create();
						if (isFragmentReady(act)) dlg.show();
					}
				}); else {
					final Activity act = getActivity();
					if (isFragmentReady(act) && !miuizerModuleActive)
					act.runOnUiThread(new Runnable() {
						public void run() {
							showXposedDialog(act);
						}
					});
				}
/*
				String miuizerPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SenseToolbox/";
				HttpURLConnection connection = null;

				try {
					URL url = new URL("http://sensetoolbox.com/last_build");
					connection = (HttpURLConnection)url.openConnection();
					connection.setDefaultUseCaches(false);
					connection.setUseCaches(false);
					connection.setRequestProperty("Pragma", "no-cache");
					connection.setRequestProperty("Cache-Control", "no-cache");
					connection.setRequestProperty("Expires", "-1");
					connection.connect();

					if (connection.getResponseCode() == HttpURLConnection.HTTP_OK || connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
						String last_build = "";

						try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
							last_build = reader.readLine().trim();
						} catch (Exception e) { e.printStackTrace(); }

						File tmp = new File(miuizerPath);
						if (!tmp.exists()) tmp.mkdirs();
						try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(miuizerPath + "last_build", false))) {
							writer.write(last_build);
						} catch (Exception e) { e.printStackTrace(); }
					}
				} catch (Exception e) {}

				try {
					if (connection != null) connection.disconnect();
				} catch (Exception e) {}

				try (InputStream inputFile = new FileInputStream(miuizerPath + "last_build")) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile));
					int last_build = 0;
					try {
						last_build = Integer.parseInt(reader.readLine().trim());
					} catch (Exception e) {}

					if (last_build != 0 && Helpers.buildVersion < last_build)
						handler.post(showUpdateNotification);
					else
						handler.post(hideUpdateNotification);
				} catch (Exception e) {}
*/
			}
		}).start();

		if (Helpers.prefs.getBoolean("pref_key_was_restore", false)) {
			Helpers.prefs.edit().putBoolean("pref_key_was_restore", false).apply();
			showRestoreInfoDialog();
		}

		//Add version name to support title
		try {
			PreferenceCategory supportCat = (PreferenceCategory) findPreference("pref_key_support");
			supportCat.setTitle(String.format(Helpers.l10n(act, R.string.support_version), act.getPackageManager().getPackageInfo(act.getPackageName(), 0).versionName));
		} catch (NameNotFoundException e) {
			//Shouldn't happen...
			e.printStackTrace();
		}

		CheckBoxPreference.OnPreferenceChangeListener toggleIcon = new CheckBoxPreference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PackageManager pm = act.getPackageManager();
				if ((Boolean)newValue)
					pm.setComponentEnabledSetting(new ComponentName(act, GateWay.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
				else
					pm.setComponentEnabledSetting(new ComponentName(act, GateWay.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
				return true;
			}
		};

		ListPreference.OnPreferenceChangeListener changeBackgroundColor = new ListPreference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (act != null & !act.isFinishing()) ((ActivityEx)act).updateTheme(Integer.parseInt((String)newValue));
				return true;
			}
		};

		CheckBoxPreference.OnPreferenceClickListener openLang = new CheckBoxPreference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Helpers.openLangDialog(getActivity());
				return true;
			}
		};

		CheckBoxPreference.OnPreferenceClickListener sendCrashReport = new CheckBoxPreference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				//ACRA.getErrorReporter().handleException(null);
				return true;
			}
		};

		CheckBoxPreference.OnPreferenceClickListener openFeedbackEdit = new CheckBoxPreference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				openSubFragment(new SubFragment(), null, Helpers.SettingsType.Edit, Helpers.ActionBarType.Edit, R.string.miuizer_acramail_title, R.layout.prefs_freedback);
				return true;
			}
		};

		CheckBoxPreference miuizerSettingsPreference = (CheckBoxPreference) findPreference("pref_key_miuizer_icon");
		if (miuizerSettingsPreference != null)
		miuizerSettingsPreference.setOnPreferenceChangeListener(toggleIcon);
		Preference miuizerBackgroundColorPreference = findPreference("pref_key_miuizer_material_background");
		if (miuizerBackgroundColorPreference != null)
		miuizerBackgroundColorPreference.setOnPreferenceChangeListener(changeBackgroundColor);
		Preference miuizerLanguagePreference = findPreference("pref_key_miuizer_lang");
		if (miuizerLanguagePreference != null)
		miuizerLanguagePreference.setOnPreferenceClickListener(openLang);
		Preference miuizerCrashReportPreference = findPreference("pref_key_miuizer_sendreport");
		if (miuizerCrashReportPreference != null)
		miuizerCrashReportPreference.setOnPreferenceClickListener(sendCrashReport);
		Preference feedbackPreference = findPreference("pref_key_miuizer_feedback");
		if (feedbackPreference != null)
		feedbackPreference.setOnPreferenceClickListener(openFeedbackEdit);

		Preference issueTrackerPreference = findPreference("pref_key_issuetracker");
		issueTrackerPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				Helpers.openURL(act, "https://bitbucket.org/langerhans/sense-toolbox/issues/");
				return true;
			}
		});
		Preference miuizerSitePreference = findPreference("pref_key_website");
		miuizerSitePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				Helpers.openURL(act, "http://sensetoolbox.com/");
				return true;
			}
		});
		Preference donatePagePreference = findPreference("pref_key_donatepage");
		donatePagePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference pref) {
				Helpers.openURL(act, "http://sensetoolbox.com/donate");
				return true;
			}
		});

		Helpers.removePref(this, "pref_key_miuizer_force_material", "pref_key_miuizer");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
 		getMenuInflater().inflate(R.menu.menu_mods, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.xposedinstaller:
				if (!Helpers.isXposedInstallerInstalled(getContext())) {
					Toast.makeText(getContext(), Helpers.l10n(getContext(), R.string.xposed_not_found), Toast.LENGTH_LONG).show();
					return true;
				}

				Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.solohsu.android.edxp.manager");
				try {
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					getContext().startActivity(intent);
					return true;
				} catch (Exception e1) {
					intent = new Intent("de.robv.android.xposed.installer.OPEN_SECTION");
					intent.setPackage("de.robv.android.xposed.installer");
					intent.putExtra("section", "modules");
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					try {
						getContext().startActivity(intent);
						return true;
					} catch (Exception e2) {
						Toast.makeText(getContext(), Helpers.l10n(getContext(), R.string.xposed_not_found), Toast.LENGTH_LONG).show();;
					}
					return false;
				}
			case R.id.backuprestore:
				showBackupRestoreDialog();
				return true;
			case R.id.softreboot:
				AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
				alert.setTitle(Helpers.l10n(getContext(), R.string.soft_reboot));
				alert.setMessage(Helpers.l10n(getContext(), R.string.soft_reboot_ask));
				alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						getContext().sendBroadcast(new Intent("name.mikanoshi.customiuizer.mods.action.FastReboot"));
					}
				});
				alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {}
				});
				alert.show();

			case R.id.about:
				//Intent intent = new Intent(act, MAboutScreen.class);
				//act.startActivity(intent);
				return true;
		};
		return false;
	}

	public void backupSettings(Activity act) {
		if (!Helpers.preparePathForBackup(act, Helpers.backupPath)) return;
		ObjectOutputStream output = null;
		try {
			output = new ObjectOutputStream(new FileOutputStream(Helpers.backupPath + Helpers.backupFile));
			output.writeObject(Helpers.prefs.getAll());

			AlertDialog.Builder alert = new AlertDialog.Builder(act);
			alert.setTitle(Helpers.l10n(act, R.string.do_backup));
			alert.setMessage(Helpers.l10n(act, R.string.backup_ok));
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {}
			});
			alert.show();
		} catch (Exception e) {
			AlertDialog.Builder alert = new AlertDialog.Builder(act);
			alert.setTitle(Helpers.l10n(act, R.string.warning));
			alert.setMessage(Helpers.l10n(act, R.string.storage_cannot_backup));
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {}
			});
			alert.show();

			e.printStackTrace();
		} finally {
			try {
				if (output != null) {
					output.flush();
					output.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void restoreSettings(final Activity act) {
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(Helpers.backupPath + Helpers.backupFile));
			Map<String, ?> entries = (Map<String, ?>)input.readObject();
			if (entries == null || entries.isEmpty()) throw new Exception("Cannot read entries");

			SharedPreferences.Editor prefEdit = Helpers.prefs.edit();
			prefEdit.clear();
			for (Map.Entry<String, ?> entry: entries.entrySet()) {
				Object val = entry.getValue();
				String key = entry.getKey();

				if (val instanceof Boolean)
					prefEdit.putBoolean(key, (Boolean)val);
				else if (val instanceof Float)
					prefEdit.putFloat(key, (Float)val);
				else if (val instanceof Integer)
					prefEdit.putInt(key, (Integer)val);
				else if (val instanceof Long)
					prefEdit.putLong(key, (Long)val);
				else if (val instanceof String)
					prefEdit.putString(key, ((String)val));
				else if (val instanceof Set<?>)
					prefEdit.putStringSet(key, ((Set<String>)val));
			}
			prefEdit.apply();

			AlertDialog.Builder alert = new AlertDialog.Builder(act);
			alert.setTitle(Helpers.l10n(act, R.string.do_restore));
			alert.setMessage(Helpers.l10n(act, R.string.restore_ok));
			alert.setCancelable(false);
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					act.finish();
					act.startActivity(act.getIntent());
				}
			});
			alert.show();
		} catch (Exception e) {
			AlertDialog.Builder alert = new AlertDialog.Builder(act);
			alert.setTitle(Helpers.l10n(act, R.string.warning));
			alert.setMessage(Helpers.l10n(act, R.string.storage_cannot_restore));
			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {}
			});
			alert.show();
		} finally {
			try {
				if (input != null) input.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void showBackupRestoreDialog() {
		final Activity act = getActivity();

		AlertDialog.Builder alert = new AlertDialog.Builder(act);
		alert.setTitle(Helpers.l10n(act, R.string.backup_restore));
		alert.setMessage(Helpers.l10n(act, R.string.backup_restore_choose));
		alert.setPositiveButton(Helpers.l10n(act, R.string.do_restore), new DialogInterface.OnClickListener() {
			@SuppressWarnings("unchecked")
			public void onClick(DialogInterface dialog, int whichButton) {
				if (!Helpers.checkStorageReadable(act)) return;
				restoreSettings(act);
			}
		});
		alert.setNegativeButton(Helpers.l10n(act, R.string.do_backup), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				backupSettings(act);
			}
		});
		alert.show();
	}

	// PreferenceScreens management
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen parentPreferenceScreen, Preference preference) {
		if (preference != null) {
			PreferenceCategory modsCat = (PreferenceCategory)findPreference("prefs_cat");
			if (modsCat.findPreference(preference.getKey()) != null) {

				switch (preference.getKey()) {
					case "pref_key_system":
						showNotYetDialog();
						break;
					case "pref_key_launcher":
						openSubFragment(new Launcher(), null, Helpers.SettingsType.Preference, Helpers.ActionBarType.HomeUp, R.string.launcher_mods, R.xml.prefs_launcher);
						return true;
					case "pref_key_controls":
						showNotYetDialog();
						break;
					case "pref_key_other":
						showNotYetDialog();
						break;
				}
			}
		}
		return super.onPreferenceTreeClick(parentPreferenceScreen, preference);
	}

	public void showXposedDialog(Activity act) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(Helpers.l10n(act, R.string.warning));
			builder.setMessage(Helpers.l10n(act, R.string.module_not_active));
			builder.setCancelable(true);
			builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton){}
			});
			AlertDialog dlg = builder.create();
			dlg.show();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void showRestoreInfoDialog() {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(Helpers.l10n(getActivity(), R.string.warning));
			builder.setMessage(Helpers.l10n(getActivity(), R.string.backup_restore_info));
			builder.setCancelable(true);
			builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton){}
			});
			AlertDialog dlg = builder.create();
			dlg.show();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void showNotYetDialog() {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(Helpers.l10n(getActivity(), R.string.info));
			builder.setMessage(Helpers.l10n(getActivity(), R.string.not_yet));
			builder.setCancelable(true);
			builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton){}
			});
			AlertDialog dlg = builder.create();
			dlg.show();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}