package tv.projectivy.plugin.wallpaperprovider.splashing


import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import kotlin.CharSequence

class SettingsFragment : GuidedStepSupportFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        return Guidance(
            getString(R.string.plugin_name),
            getString(R.string.plugin_description),
            getString(R.string.settings),
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_plugin)
        )
    }

    private lateinit var modeInput: GuidedAction
    private lateinit var collectionIDInput: GuidedAction
    private lateinit var searchTermInput: GuidedAction

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        PreferencesManager.init(requireContext())

        // Dropdown to select between multiple source modes:
        val modeActions = listOf(
            GuidedAction.Builder(context)
                .id(ACTION_ID_MODE_COLLECTION)
                .title(R.string.setting_mode_title_collection)
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(PreferencesManager.mode == Mode.COLLECTION.name)
                .build(),
            GuidedAction.Builder(context)
                .id(ACTION_ID_MODE_RANDOM)
                .title(R.string.setting_mode_title_random)
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(PreferencesManager.mode == Mode.RANDOM.name)
                .build(),
            GuidedAction.Builder(context)
                .id(ACTION_ID_MODE_SEARCH)
                .title(R.string.setting_mode_title_search)
                .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                .checked(PreferencesManager.mode == Mode.SEARCH.name)
                .build()
        )
        modeInput = GuidedAction.Builder(context)
            .id(ACTION_ID_MODE)
            .title(R.string.setting_mode_title)
            .description(
                when (PreferencesManager.mode) {
                    Mode.COLLECTION.name -> getString(R.string.setting_mode_title_collection)
                    Mode.RANDOM.name -> getString(R.string.setting_mode_title_random)
                    Mode.SEARCH.name -> getString(R.string.setting_mode_title_search)
                    else -> null
                }
            )
            .subActions(modeActions)
            .build()
        actions.add(modeInput)

        collectionIDInput = GuidedAction.Builder(context)
            .id(ACTION_ID_COLLECTION_ID)
            .title(R.string.setting_collection_id_title)
            .description(PreferencesManager.collectionID)
            .editDescription(PreferencesManager.collectionID)
            .descriptionEditable(true)
            .build()
        if (PreferencesManager.mode == Mode.COLLECTION.name) {
            actions.add(collectionIDInput)
        }

        searchTermInput = GuidedAction.Builder(context)
            .id(ACTION_ID_SEARCH_TERM)
            .title(R.string.setting_search_term_title)
            .description(PreferencesManager.searchTerm)
            .editDescription(PreferencesManager.searchTerm)
            .descriptionEditable(true)
            .build()
        if (PreferencesManager.mode == Mode.SEARCH.name) {
            actions.add(searchTermInput)
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_ID_COLLECTION_ID -> {
                val params: CharSequence? = action.editDescription
                findActionById(ACTION_ID_COLLECTION_ID)?.description = params
                notifyActionChanged(findActionPositionById(ACTION_ID_COLLECTION_ID))
                PreferencesManager.collectionID = params.toString()
            }

            ACTION_ID_SEARCH_TERM -> {
                val params: CharSequence? = action.editDescription
                findActionById(ACTION_ID_SEARCH_TERM)?.description = params
                notifyActionChanged(findActionPositionById(ACTION_ID_SEARCH_TERM))
                PreferencesManager.searchTerm = params.toString()
            }
        }
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        when (action.id) {
            ACTION_ID_MODE_COLLECTION -> {
                PreferencesManager.mode = Mode.COLLECTION.name
            }

            ACTION_ID_MODE_RANDOM -> {
                PreferencesManager.mode = Mode.RANDOM.name
            }

            ACTION_ID_MODE_SEARCH -> {
                PreferencesManager.mode = Mode.SEARCH.name

            }
        }
        refreshUI()
        return true
    }

    private fun refreshUI() {
        if (actions.contains(collectionIDInput)) {
            actions.remove(collectionIDInput)
        }
        if (actions.contains(searchTermInput)) {
            actions.remove(searchTermInput)
        }
        when (PreferencesManager.mode) {
            Mode.COLLECTION.name -> {
                actions.add(collectionIDInput)
                modeInput.description = getString(R.string.setting_mode_title_collection)
            }

            Mode.RANDOM.name -> {
                modeInput.description = getString(R.string.setting_mode_title_random)
            }

            Mode.SEARCH.name -> {
                actions.add(searchTermInput)
                modeInput.description = getString(R.string.setting_mode_title_search)
            }
        }
        notifyActionChanged(findActionPositionById(ACTION_ID_MODE))
        setActions(actions.toMutableList()) // Rebind the modified action list to the fragment
    }

    companion object {
        private const val ACTION_ID_COLLECTION_ID = 1L
        private const val ACTION_ID_MODE = 2L
        private const val ACTION_ID_MODE_COLLECTION = 3L
        private const val ACTION_ID_MODE_RANDOM = 4L
        private const val ACTION_ID_MODE_SEARCH = 5L
        private const val ACTION_ID_SEARCH_TERM = 6L
    }
}
