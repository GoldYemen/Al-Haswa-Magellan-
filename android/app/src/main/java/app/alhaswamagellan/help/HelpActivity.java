package app.alhaswamagellan.help;

import androidx.fragment.app.Fragment;
import app.alhaswamagellan.base.BaseToolbarActivity;

public class HelpActivity extends BaseToolbarActivity
{
  @Override
  protected Class<? extends Fragment> getFragmentClass()
  {
    return HelpFragment.class;
  }
}
