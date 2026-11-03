package app.alhaswamagellan.help;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import app.alhaswamagellan.BuildConfig;
import app.alhaswamagellan.R;
import app.alhaswamagellan.base.BaseMwmFragment;
import app.alhaswamagellan.sdk.Framework;
import app.alhaswamagellan.sdk.util.Constants;
import app.alhaswamagellan.sdk.util.DateUtils;
import app.alhaswamagellan.util.Graphics;
import app.alhaswamagellan.util.SharingUtils;
import app.alhaswamagellan.util.Utils;
import app.alhaswamagellan.util.WindowInsetUtils.ScrollableContentInsetsListener;

public class HelpFragment extends BaseMwmFragment implements View.OnClickListener
{
  private ActivityResultLauncher<SharingUtils.SharingIntent> shareLauncher;

  private TextView setupItem(@IdRes int id, boolean tint, @NonNull View frame)
  {
    final TextView view = frame.findViewById(id);
    view.setOnClickListener(this);
    if (tint)
      Graphics.tint(view);
    return view;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    View root = inflater.inflate(R.layout.about, container, false);

    ((TextView) root.findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME);

    final boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

    final String dataVersion = DateUtils.getShortDateFormatter().format(Framework.getDataVersion());
    final TextView osmPresentationView = root.findViewById(R.id.osm_presentation);
    if (osmPresentationView != null)
      osmPresentationView.setText(getString(R.string.osm_presentation, dataVersion));

    setupItem(R.id.web, true, root);
    setupItem(R.id.email, true, root);
    setupItem(R.id.github, true, root);
    setupItem(R.id.openstreetmap, true, root);
    setupItem(R.id.report, isLandscape, root);
    setupItem(R.id.copyright, false, root);

    View termOfUseView = root.findViewById(R.id.term_of_use_link);
    View privacyPolicyView = root.findViewById(R.id.privacy_policy);
    termOfUseView.setOnClickListener(
        v -> Utils.openUrl(requireActivity(), getResources().getString(R.string.translated_om_site_url) + "terms/"));
    privacyPolicyView.setOnClickListener(
        v -> Utils.openUrl(requireActivity(), getResources().getString(R.string.translated_om_site_url) + "privacy/"));

    shareLauncher = SharingUtils.RegisterLauncher(this);

    ViewCompat.setOnApplyWindowInsetsListener(root, new ScrollableContentInsetsListener(root));

    return root;
  }

  @Override
  public void onClick(View v)
  {
    final int id = v.getId();
    if (id == R.id.web)
      Utils.openUrl(requireActivity(), "https://al-haswa-magellan-website.vercel.app/");
    else if (id == R.id.email)
      Utils.sendTo(requireContext(), BuildConfig.SUPPORT_MAIL, "Al-Haswa Magellan");
    else if (id == R.id.github)
      Utils.openUrl(requireActivity(), Constants.Url.GITHUB);
    else if (id == R.id.openstreetmap)
      Utils.openUrl(requireActivity(), getString(R.string.osm_wiki_about_url));
    else if (id == R.id.report)
      Utils.sendBugReport(shareLauncher, requireActivity(), "", "");
    else if (id == R.id.copyright)
      ((HelpActivity) requireActivity()).stackFragment(CopyrightFragment.class, getString(R.string.copyright), null);
  }
}
