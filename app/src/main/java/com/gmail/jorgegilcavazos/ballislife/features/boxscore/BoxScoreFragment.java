package com.gmail.jorgegilcavazos.ballislife.features.boxscore;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreTeam;
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreValues;
import com.gmail.jorgegilcavazos.ballislife.features.model.StatLine;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;
import com.gmail.jorgegilcavazos.ballislife.util.ThemeUtils;
import com.gmail.jorgegilcavazos.ballislife.util.UnitUtils;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity
        .AWAY_TEAM_KEY;
import static com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity
        .HOME_TEAM_KEY;


public class BoxScoreFragment extends Fragment implements BoxScoreView {

    @Inject BoxScorePresenter presenter;
    @Inject LocalRepository localRepository;

    @BindView(R.id.button_home) Button btnHome;
    @BindView(R.id.button_away) Button btnAway;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.text_load_message) TextView tvLoadMessage;
    @BindView(R.id.playersTable) TableLayout playersTable;
    @BindView(R.id.statsTable) TableLayout statsTable;
    @BindView(R.id.scrollView) ScrollView scrollView;

    private Unbinder unbinder;

    private String homeTeam;
    private String awayTeam;
    private String gameId;
    private BoxScoreSelectedTeam teamSelected;
    private int textColor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        BallIsLifeApplication.getAppComponent().inject(this);

        if (getArguments() != null) {
            homeTeam = getArguments().getString(HOME_TEAM_KEY);
            awayTeam = getArguments().getString(AWAY_TEAM_KEY);
            gameId = getArguments().getString(CommentsActivity.GAME_ID_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_box_score, container, false);
        unbinder = ButterKnife.bind(this, view);

        teamSelected = BoxScoreSelectedTeam.HOME;
        setHomeAwayBackground();

        btnAway.setText(awayTeam);
        btnHome.setText(homeTeam);

        textColor = ThemeUtils.Companion.getTextColor(getActivity(), localRepository.getAppTheme());

        presenter.attachView(this);
        presenter.loadBoxScore(gameId, teamSelected, true /* forceNetwork */);

        return view;
    }

    @Override
    public void onDestroyView() {
        presenter.detachView();
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_box_score, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                presenter.loadBoxScore(gameId, teamSelected, true /* forceNetwork */);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.button_away)
    public void onButtonAwayClick() {
        btnAway.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        btnHome.setTextColor(textColor);

        teamSelected = BoxScoreSelectedTeam.VISITOR;
        setHomeAwayBackground();

        presenter.loadBoxScore(gameId, teamSelected, false /* forceNetwork */);
    }

    @OnClick(R.id.button_home)
    public void onButtonHomeClick() {
        btnHome.setTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        btnAway.setTextColor(textColor);

        teamSelected = BoxScoreSelectedTeam.HOME;
        setHomeAwayBackground();

        presenter.loadBoxScore(gameId, teamSelected, false /* forceNetwork */);
    }

    public void showTeamBoxScore(BoxScoreTeam team){
        List<String> players = new ArrayList<>();

        for (StatLine statLine : team.getPstsg()) {
            // Some players don't have a first name, like Nene.
            if (statLine.getFn() != null && statLine.getFn().length() >= 1) {
                players.add(statLine.getFn().substring(0, 1) + ". " + statLine.getLn());
            } else {
                players.add(statLine.getLn());
            }
        }
        players.add("TOTAL");

        int i = 1;
        addRowToPlayersTable2("PLAYER");
        for (String player : players) {
            addRowToPlayersTable2(player);
            if (i == 5 || i == players.size()-1) {
                addSeparatorRowToPlayers();
            }
            i++;
        }

        StatLine total = new StatLine(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,"0","0");
        i = 1;
        addRowToStatsTable2(Optional.absent());
        for (StatLine statLine : team.getPstsg()) {
            addRowToStatsTable2(Optional.of(statLine));
            addToTeamTotalStats(statLine, total);
            if (i == 5 || i == players.size()-1) {
                addSeparatorRowToStats(19);
            }
            i++;
        }
        displayTeamTotalStats(total);
        scrollView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showVisitorBoxScore(@NonNull BoxScoreValues values) {
        btnHome.setText(getString(R.string.box_score_team_score, homeTeam,
                values.getHls().getScore()));
        btnAway.setText(getString(R.string.box_score_team_score, awayTeam,
                values.getVls().getScore()));

        showTeamBoxScore(values.getVls());
    }

    @Override
    public void showHomeBoxScore(BoxScoreValues values) {
        btnHome.setText(getString(R.string.box_score_team_score, homeTeam,
                values.getHls().getScore()));
        btnAway.setText(getString(R.string.box_score_team_score, awayTeam,
                values.getVls().getScore()));

        showTeamBoxScore(values.getHls());
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        if (active) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void hideBoxScore() {
        scrollView.setVisibility(View.GONE);
        playersTable.removeAllViews();
        statsTable.removeAllViews();
    }

    @Override
    public void showBoxScoreNotAvailableMessage(boolean active) {
        if (active) {
            tvLoadMessage.setText(R.string.box_score_not_available);
            tvLoadMessage.setVisibility(View.VISIBLE);
        } else {
            tvLoadMessage.setVisibility(View.GONE);
        }
    }

    public void addRowToPlayersTable2(String content) {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        int width = (int) UnitUtils.convertDpToPixel(100, getActivity());
        row.setMinimumWidth(width);

        if (content.equals("PLAYER")) {
            TextView tv = addHeaderItem(row, content);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            row.addView(tv);
        } else {
            TextView tv = addNormalItem(row, content);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            row.addView(tv);
        }

        playersTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }


    public void displayTeamTotalStats(StatLine statLine){
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        row.addView(addNormalItem(row, String.valueOf(statLine.getMin())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getPts())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getReb())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getAst())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getStl())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getBlk())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getBlka())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFgm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFga())));
        row.addView(addNormalItem(row, getShootingPct(statLine.getFga(), statLine.getFgm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getTpm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getTpa())));
        row.addView(addNormalItem(row, getShootingPct(statLine.getTpa(), statLine.getTpm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFtm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getFta())));
        row.addView(addNormalItem(row, getShootingPct(statLine.getFta(), statLine.getFtm())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getPf())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getTov())));
        row.addView(addNormalItem(row, String.valueOf(statLine.getPm())));

        statsTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    public void addToTeamTotalStats(StatLine curr, StatLine total){
        total.setMin(curr.getMin()+total.getMin());
        total.setPts(curr.getPts()+total.getPts());
        total.setReb(curr.getReb()+total.getReb());
        total.setAst(curr.getAst()+total.getAst());
        total.setStl(curr.getStl()+total.getStl());
        total.setBlk(curr.getBlk()+total.getBlk());
        total.setBlka(curr.getBlka()+total.getBlka());
        total.setFgm(curr.getFgm()+total.getFgm());
        total.setFga(curr.getFga()+total.getFga());
        total.setTpm(curr.getTpm()+total.getTpm());
        total.setTpa(curr.getTpa()+total.getTpa());
        total.setFtm(curr.getFtm()+total.getFtm());
        total.setFta(curr.getFta()+total.getFta());
        total.setPf(curr.getPf()+total.getPf());
        total.setTov(curr.getTov()+total.getTov());
        total.setPm(curr.getPm()+total.getPm());
    }
    
    public void addRowToStatsTable2(Optional<StatLine> statLineOptional) {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        if (statLineOptional.isPresent()) {
            StatLine statLine = statLineOptional.get();

            row.addView(addNormalItem(row, String.valueOf(statLine.getMin())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getPts())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getReb())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getAst())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getStl())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getBlk())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getBlka())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFgm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFga())));
            row.addView(addNormalItem(row, getShootingPct(statLine.getFga(), statLine.getFgm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getTpm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getTpa())));
            row.addView(addNormalItem(row, getShootingPct(statLine.getTpa(), statLine.getTpm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFtm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getFta())));
            row.addView(addNormalItem(row, getShootingPct(statLine.getFta(), statLine.getFtm())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getPf())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getTov())));
            row.addView(addNormalItem(row, String.valueOf(statLine.getPm())));
        } else {
            row.addView(addHeaderItem(row, "MIN"));
            row.addView(addHeaderItem(row, "PTS"));
            row.addView(addHeaderItem(row, "REB"));
            row.addView(addHeaderItem(row, "AST"));
            row.addView(addHeaderItem(row, "STL"));
            row.addView(addHeaderItem(row, "BLK"));
            row.addView(addHeaderItem(row, "BA"));
            row.addView(addHeaderItem(row, "FGM"));
            row.addView(addHeaderItem(row, "FGA"));
            row.addView(addHeaderItem(row, "FG%"));
            row.addView(addHeaderItem(row, "3PM"));
            row.addView(addHeaderItem(row, "3PA"));
            row.addView(addHeaderItem(row, "3P%"));
            row.addView(addHeaderItem(row, "FTM"));
            row.addView(addHeaderItem(row, "FTA"));
            row.addView(addHeaderItem(row, "FT%"));
            row.addView(addHeaderItem(row, "PF"));
            row.addView(addHeaderItem(row, "TO"));
            row.addView(addHeaderItem(row, "+/-"));
        }

        statsTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    private void addSeparatorRowToPlayers() {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.box_score_separator, row, false);
        row.addView(view);
        playersTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    private void addSeparatorRowToStats(int columns) {
        TableRow row = new TableRow(getActivity());
        row.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        for (int i = 0; i < columns; i++) {
            View view = LayoutInflater.from(getActivity())
                    .inflate(R.layout.box_score_separator, row, false);
            row.addView(view);
        }
        statsTable.addView(row, new TableLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
    }

    private TextView addHeaderItem(TableRow row, String text) {
        TextView view = (TextView) LayoutInflater.from(getActivity())
                .inflate(R.layout.boxscore_item, row, false);
        view.setText(text);
        view.setTypeface(null, Typeface.BOLD);
        view.setMinWidth((int) UnitUtils.convertDpToPixel(30, getActivity()));
        return view;
    }

    private TextView addNormalItem(TableRow row, String text) {
        TextView view = (TextView) LayoutInflater.from(getActivity())
                .inflate(R.layout.boxscore_item, row, false);
        view.setText(text);
        view.setTypeface(null, Typeface.NORMAL);
        view.setMinWidth((int) UnitUtils.convertDpToPixel(30, getActivity()));
        return view;
    }

    private String getShootingPct(double attempts, double makes) {
        if (attempts == 0) {
            return "-";
        }

        int pct = (int) ((makes / attempts) * 100);
        return pct + "%";
    }

    private void setHomeAwayBackground() {
        if (teamSelected == BoxScoreSelectedTeam.VISITOR) {
            if (localRepository.getAppTheme() == SwishTheme.DARK) {
                btnAway.setBackgroundResource(R.drawable.square_black_dark);
                btnHome.setBackgroundResource(R.drawable.square_white_dark);
            } else {
                btnAway.setBackgroundResource(R.drawable.square_black);
                btnHome.setBackgroundResource(R.drawable.square_white);
            }
        } else {
            if (localRepository.getAppTheme() == SwishTheme.DARK) {
                btnHome.setBackgroundResource(R.drawable.square_black_dark);
                btnAway.setBackgroundResource(R.drawable.square_white_dark);
            } else {
                btnHome.setBackgroundResource(R.drawable.square_black);
                btnAway.setBackgroundResource(R.drawable.square_white);
            }
        }
    }

    @Override
    public void showUnknownErrorToast(int code) {
        Toast.makeText(getActivity(), getString(R.string.something_went_wrong, code),
                Toast.LENGTH_SHORT).show();
    }
}
