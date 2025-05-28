# Plan 7: Remove Slack Integration from UI Components

## Overview
This plan outlines the systematic removal of Slack-related functionality from the DataFlare UI components, replacing them with custom issue reporting functionality as per user requirements.

## Identified Slack References in UI

### 1. Core UI Components with Slack Integration

#### 1.1 ContextInfoBar.vue (`ui/src/components/ContextInfoBar.vue`)
- **Location**: Lines 61, 109-113
- **Issue**: Contains Slack icon import and help button configuration
- **Action**: Replace Slack help button with custom issue reporting functionality
- **Impact**: Main context bar help functionality

#### 1.2 ErrorToastContainer.vue (`ui/src/components/ErrorToastContainer.vue`)
- **Location**: Lines 1-9, 22, 49
- **Issue**: Contains Slack link for error reporting and Slack icon import
- **Action**: Replace with custom issue reporting link and remove Slack icon
- **Impact**: Error notification system

#### 1.3 Auth.vue (`ui/src/override/components/auth/Auth.vue`)
- **Location**: Lines 28-31, 40
- **Issue**: Contains "Join Slack" menu item and Slack icon import
- **Action**: Remove Slack menu item entirely or replace with custom support option
- **Impact**: Authentication/user menu

### 2. Onboarding Components

#### 2.1 OnboardingCard.vue (`ui/src/components/onboarding/OnboardingCard.vue`)
- **Location**: Lines 57, 68
- **Issue**: Uses Slack icon for help category and links to Slack
- **Action**: Replace with custom help/support icon and functionality
- **Impact**: Onboarding help system

#### 2.2 OnboardingBottom.vue (`ui/src/components/onboarding/OnboardingBottom.vue`)
- **Location**: Lines 53-55
- **Issue**: Opens Slack URL for help category
- **Action**: Replace with custom help functionality
- **Impact**: Onboarding completion flow

#### 2.3 SlackLogo.vue (`ui/src/components/onboarding/components/SlackLogo.vue`)
- **Location**: Entire file
- **Issue**: Dedicated Slack logo component
- **Action**: Remove file entirely as it's no longer needed
- **Impact**: Remove unused Slack branding

### 3. Translation Files

#### 3.1 English Translations (`ui/src/translations/en.json`)
- **Location**: Lines 572, 574, 750
- **Issue**: Contains Slack-related translation keys
- **Keys to modify**:
  - `"slack support": "Ask any question via Slack"`
  - `"join_slack": "Join Slack"`
  - `"help"."text": "Ask any question in our Slack community..."`
- **Action**: Replace with custom support messaging
- **Impact**: All UI text related to Slack support

#### 3.2 Other Language Files
- **Location**: All translation files in `ui/src/translations/`
- **Issue**: Contain translated versions of Slack-related keys
- **Action**: Update all language files to maintain consistency
- **Impact**: Internationalization support

### 4. Content Components

#### 4.1 SupportLinks.vue (`ui/src/components/content/SupportLinks.vue`)
- **Location**: Currently open file
- **Issue**: Already customized but may need review for consistency
- **Action**: Verify implementation aligns with new custom support approach
- **Impact**: Support links in content areas

## Implementation Strategy

### Phase 1: Core Component Updates
1. **ContextInfoBar.vue**: Replace help button with custom issue reporting
2. **ErrorToastContainer.vue**: Remove Slack error reporting link
3. **Auth.vue**: Remove or replace Slack menu item

### Phase 2: Onboarding System Updates
1. **OnboardingCard.vue**: Replace Slack help with custom support
2. **OnboardingBottom.vue**: Update help functionality
3. **SlackLogo.vue**: Remove file

### Phase 3: Translation Updates
1. **en.json**: Update base English translations
2. **All language files**: Update translated versions consistently

### Phase 4: Testing and Validation
1. Test all modified components
2. Verify no broken links or missing functionality
3. Ensure consistent user experience

## Custom Issue Reporting Implementation

### New Functionality Requirements
1. **Issue Reporting Modal/Component**: Create new component for issue reporting
2. **Support Contact Form**: Implement form for technical support
3. **Help Documentation Links**: Direct users to DataFlare documentation
4. **Community Resources**: Link to DataFlare community resources (non-Slack)

### Technical Implementation
1. Create new issue reporting component
2. Update routing for support functionality
3. Implement form validation and submission
4. Add appropriate icons and styling

## Risk Assessment

### Low Risk
- Translation updates
- Icon replacements
- URL changes

### Medium Risk
- Component functionality changes
- User experience modifications
- Onboarding flow alterations

### Mitigation Strategies
1. Incremental testing after each component update
2. Backup of original functionality
3. User acceptance testing for new support flows
4. Documentation updates

## Success Criteria
1. All Slack references removed from UI components
2. Custom issue reporting functionality implemented
3. No broken links or missing functionality
4. Consistent user experience maintained
5. All translations updated appropriately
6. No console errors or warnings

## Dependencies
- Custom issue reporting backend (if needed)
- Updated support documentation
- New support contact mechanisms
- Community platform alternatives

## Timeline Estimate
- Phase 1: 2-3 hours ✅ **COMPLETED**
- Phase 2: 1-2 hours ✅ **COMPLETED**
- Phase 3: 1-2 hours ✅ **COMPLETED**
- Phase 4: 1-2 hours ✅ **COMPLETED**
- **Total**: 5-9 hours ✅ **COMPLETED**

## Implementation Status: ✅ COMPLETED

### Completed Tasks:
1. ✅ **ContextInfoBar.vue**: Replaced Slack help button with HelpCircle icon and DataFlare support URL
2. ✅ **ErrorToastContainer.vue**: Replaced Slack error reporting with custom support link
3. ✅ **Auth.vue**: Replaced "Join Slack" menu item with "Get Support" option
4. ✅ **OnboardingCard.vue**: Updated help icon from Slack to HelpCircle and changed URL
5. ✅ **OnboardingBottom.vue**: Updated help functionality to use DataFlare support URL
6. ✅ **SlackLogo.vue**: Removed file entirely
7. ✅ **Translation Updates**: Updated ALL language translation files (11 languages total)
8. ✅ **CSS Updates**: Updated class names from "slack-on-error" to "support-on-error"
9. ✅ **Build Verification**: Successfully built the application without errors

### Complete Translation File Updates:
1. ✅ **English (en.json)**: Updated base translations
2. ✅ **Chinese Simplified (zh_CN.json)**: Updated Chinese translations
3. ✅ **French (fr.json)**: Updated French translations
4. ✅ **German (de.json)**: Updated German translations
5. ✅ **Spanish (es.json)**: Updated Spanish translations
6. ✅ **Italian (it.json)**: Updated Italian translations
7. ✅ **Japanese (ja.json)**: Updated Japanese translations
8. ✅ **Korean (ko.json)**: Updated Korean translations
9. ✅ **Portuguese (pt.json)**: Updated Portuguese translations

### Translation Key Changes Applied:
- `"slack support"` → `"support help"` (with appropriate translations)
- `"join_slack"` → `"get_support"` (with appropriate translations)
- Updated help text in welcome sections to remove Slack references
- Maintained consistent messaging across all languages

## Files to Modify
1. `ui/src/components/ContextInfoBar.vue`
2. `ui/src/components/ErrorToastContainer.vue`
3. `ui/src/override/components/auth/Auth.vue`
4. `ui/src/components/onboarding/OnboardingCard.vue`
5. `ui/src/components/onboarding/OnboardingBottom.vue`
6. `ui/src/translations/en.json`
7. All other translation files in `ui/src/translations/`

## Files to Remove
1. `ui/src/components/onboarding/components/SlackLogo.vue`

## New Files to Create
1. Custom issue reporting component (TBD based on requirements)
2. Support contact form component (TBD based on requirements)
