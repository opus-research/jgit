/*
 * Copyright (C) 2010, 2013 Sasa Zivkov <sasa.zivkov@sap.com>
 * Copyright (C) 2012, Research In Motion Limited
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.internal;

import org.eclipse.jgit.nls.NLS;
import org.eclipse.jgit.nls.TranslationBundle;

/**
 * Translation bundle for JGit core
 */
public class JGitText extends TranslationBundle {

	/**
	 * @return an instance of this translation bundle
	 */
	public static JGitText get() {
		return NLS.getBundleFor(JGitText.class);
	}

	// @formatter:off
	/***/ public String abbreviationLengthMustBeNonNegative;
	/***/ public String abortingRebase;
	/***/ public String abortingRebaseFailed;
	/***/ public String abortingRebaseFailedNoOrigHead;
	/***/ public String advertisementCameBefore;
	/***/ public String advertisementOfCameBefore;
	/***/ public String amazonS3ActionFailed;
	/***/ public String amazonS3ActionFailedGivingUp;
	/***/ public String ambiguousObjectAbbreviation;
	/***/ public String aNewObjectIdIsRequired;
	/***/ public String anExceptionOccurredWhileTryingToAddTheIdOfHEAD;
	/***/ public String anSSHSessionHasBeenAlreadyCreated;
	/***/ public String applyingCommit;
	/***/ public String archiveFormatAlreadyAbsent;
	/***/ public String archiveFormatAlreadyRegistered;
	/***/ public String argumentIsNotAValidCommentString;
	/***/ public String atLeastOnePathIsRequired;
	/***/ public String atLeastOnePatternIsRequired;
	/***/ public String atLeastTwoFiltersNeeded;
	/***/ public String atomicPushNotSupported;
	/***/ public String atomicRefUpdatesNotSupported;
	/***/ public String authenticationNotSupported;
	/***/ public String badBase64InputCharacterAt;
	/***/ public String badEntryDelimiter;
	/***/ public String badEntryName;
	/***/ public String badEscape;
	/***/ public String badGroupHeader;
	/***/ public String badObjectType;
	/***/ public String badRef;
	/***/ public String badSectionEntry;
	/***/ public String badShallowLine;
	/***/ public String bareRepositoryNoWorkdirAndIndex;
	/***/ public String base64InputNotProperlyPadded;
	/***/ public String baseLengthIncorrect;
	/***/ public String bitmapMissingObject;
	/***/ public String bitmapsMustBePrepared;
	/***/ public String blameNotCommittedYet;
	/***/ public String blobNotFound;
	/***/ public String blobNotFoundForPath;
	/***/ public String blockSizeNotPowerOf2;
	/***/ public String branchNameInvalid;
	/***/ public String buildingBitmaps;
	/***/ public String cachedPacksPreventsIndexCreation;
	/***/ public String cachedPacksPreventsListingObjects;
	/***/ public String cannotBeCombined;
	/***/ public String cannotBeRecursiveWhenTreesAreIncluded;
	/***/ public String cannotChangeActionOnComment;
	/***/ public String cannotChangeToComment;
	/***/ public String cannotCheckoutFromUnbornBranch;
	/***/ public String cannotCheckoutOursSwitchBranch;
	/***/ public String cannotCombineSquashWithNoff;
	/***/ public String cannotCombineTreeFilterWithRevFilter;
	/***/ public String cannotCommitOnARepoWithState;
	/***/ public String cannotCommitWriteTo;
	/***/ public String cannotConnectPipes;
	/***/ public String cannotConvertScriptToText;
	/***/ public String cannotCreateConfig;
	/***/ public String cannotCreateDirectory;
	/***/ public String cannotCreateHEAD;
	/***/ public String cannotCreateIndexfile;
	/***/ public String cannotCreateTempDir;
	/***/ public String cannotDeleteCheckedOutBranch;
	/***/ public String cannotDeleteFile;
	/***/ public String cannotDeleteObjectsPath;
	/***/ public String cannotDeleteStaleTrackingRef;
	/***/ public String cannotDeleteStaleTrackingRef2;
	/***/ public String cannotDetermineProxyFor;
	/***/ public String cannotDownload;
	/***/ public String cannotEnterObjectsPath;
	/***/ public String cannotEnterPathFromParent;
	/***/ public String cannotExecute;
	/***/ public String cannotGet;
	/***/ public String cannotGetObjectsPath;
	/***/ public String cannotListObjectsPath;
	/***/ public String cannotListPackPath;
	/***/ public String cannotListRefs;
	/***/ public String cannotLock;
	/***/ public String cannotLockPackIn;
	/***/ public String cannotMatchOnEmptyString;
	/***/ public String cannotMkdirObjectPath;
	/***/ public String cannotMoveIndexTo;
	/***/ public String cannotMovePackTo;
	/***/ public String cannotOpenService;
	/***/ public String cannotParseDate;
	/***/ public String cannotParseGitURIish;
	/***/ public String cannotPullOnARepoWithState;
	/***/ public String cannotRead;
	/***/ public String cannotReadBlob;
	/***/ public String cannotReadCommit;
	/***/ public String cannotReadFile;
	/***/ public String cannotReadHEAD;
	/***/ public String cannotReadIndex;
	/***/ public String cannotReadObject;
	/***/ public String cannotReadObjectsPath;
	/***/ public String cannotReadTree;
	/***/ public String cannotRebaseWithoutCurrentHead;
	/***/ public String cannotResolveLocalTrackingRefForUpdating;
	/***/ public String cannotSquashFixupWithoutPreviousCommit;
	/***/ public String cannotStoreObjects;
	/***/ public String cannotResolveUniquelyAbbrevObjectId;
	/***/ public String cannotUnloadAModifiedTree;
	/***/ public String cannotUpdateUnbornBranch;
	/***/ public String cannotWorkWithOtherStagesThanZeroRightNow;
	/***/ public String cannotWriteObjectsPath;
	/***/ public String canOnlyCherryPickCommitsWithOneParent;
	/***/ public String canOnlyRevertCommitsWithOneParent;
	/***/ public String commitDoesNotHaveGivenParent;
	/***/ public String cantFindObjectInReversePackIndexForTheSpecifiedOffset;
	/***/ public String cantPassMeATree;
	/***/ public String channelMustBeInRange1_255;
	/***/ public String characterClassIsNotSupported;
	/***/ public String checkoutConflictWithFile;
	/***/ public String checkoutConflictWithFiles;
	/***/ public String checkoutUnexpectedResult;
	/***/ public String classCastNotA;
	/***/ public String cloneNonEmptyDirectory;
	/***/ public String closed;
	/***/ public String collisionOn;
	/***/ public String commandRejectedByHook;
	/***/ public String commandWasCalledInTheWrongState;
	/***/ public String commitAlreadyExists;
	/***/ public String commitMessageNotSpecified;
	/***/ public String commitOnRepoWithoutHEADCurrentlyNotSupported;
	/***/ public String commitAmendOnInitialNotPossible;
	/***/ public String compressingObjects;
	/***/ public String connectionFailed;
	/***/ public String connectionTimeOut;
	/***/ public String contextMustBeNonNegative;
	/***/ public String corruptionDetectedReReadingAt;
	/***/ public String corruptObjectBadDate;
	/***/ public String corruptObjectBadEmail;
	/***/ public String corruptObjectBadStream;
	/***/ public String corruptObjectBadStreamCorruptHeader;
	/***/ public String corruptObjectBadTimezone;
	/***/ public String corruptObjectDuplicateEntryNames;
	/***/ public String corruptObjectGarbageAfterSize;
	/***/ public String corruptObjectIncorrectLength;
	/***/ public String corruptObjectIncorrectSorting;
	/***/ public String corruptObjectInvalidEntryMode;
	/***/ public String corruptObjectInvalidMode;
	/***/ public String corruptObjectInvalidModeChar;
	/***/ public String corruptObjectInvalidModeStartsZero;
	/***/ public String corruptObjectInvalidMode2;
	/***/ public String corruptObjectInvalidMode3;
	/***/ public String corruptObjectInvalidName;
	/***/ public String corruptObjectInvalidNameAux;
	/***/ public String corruptObjectInvalidNameCon;
	/***/ public String corruptObjectInvalidNameCom;
	/***/ public String corruptObjectInvalidNameEnd;
	/***/ public String corruptObjectInvalidNameIgnorableUnicode;
	/***/ public String corruptObjectInvalidNameInvalidUtf8;
	/***/ public String corruptObjectInvalidNameLpt;
	/***/ public String corruptObjectInvalidNameNul;
	/***/ public String corruptObjectInvalidNamePrn;
	/***/ public String corruptObjectInvalidObject;
	/***/ public String corruptObjectInvalidParent;
	/***/ public String corruptObjectInvalidTree;
	/***/ public String corruptObjectInvalidType;
	/***/ public String corruptObjectInvalidType2;
	/***/ public String corruptObjectMalformedHeader;
	/***/ public String corruptObjectMissingEmail;
	/***/ public String corruptObjectNameContainsByte;
	/***/ public String corruptObjectNameContainsChar;
	/***/ public String corruptObjectNameContainsNullByte;
	/***/ public String corruptObjectNameContainsSlash;
	/***/ public String corruptObjectNameDot;
	/***/ public String corruptObjectNameDotDot;
	/***/ public String corruptObjectNameZeroLength;
	/***/ public String corruptObjectNegativeSize;
	/***/ public String corruptObjectNoAuthor;
	/***/ public String corruptObjectNoCommitter;
	/***/ public String corruptObjectNoHeader;
	/***/ public String corruptObjectNoObject;
	/***/ public String corruptObjectNoObjectHeader;
	/***/ public String corruptObjectNoTaggerBadHeader;
	/***/ public String corruptObjectNoTaggerHeader;
	/***/ public String corruptObjectNoTagHeader;
	/***/ public String corruptObjectNoTagName;
	/***/ public String corruptObjectNotree;
	/***/ public String corruptObjectNotreeHeader;
	/***/ public String corruptObjectNoType;
	/***/ public String corruptObjectNoTypeHeader;
	/***/ public String corruptObjectPackfileChecksumIncorrect;
	/***/ public String corruptObjectTruncatedInMode;
	/***/ public String corruptObjectTruncatedInName;
	/***/ public String corruptObjectTruncatedInObjectId;
	/***/ public String corruptObjectZeroId;
	/***/ public String corruptPack;
	/***/ public String corruptUseCnt;
	/***/ public String couldNotCheckOutBecauseOfConflicts;
	/***/ public String couldNotDeleteLockFileShouldNotHappen;
	/***/ public String couldNotDeleteTemporaryIndexFileShouldNotHappen;
	/***/ public String couldNotGetAdvertisedRef;
	/***/ public String couldNotGetRepoStatistics;
	/***/ public String couldNotLockHEAD;
	/***/ public String couldNotReadIndexInOneGo;
	/***/ public String couldNotReadObjectWhileParsingCommit;
	/***/ public String couldNotRenameDeleteOldIndex;
	/***/ public String couldNotRenameTemporaryFile;
	/***/ public String couldNotRenameTemporaryIndexFileToIndex;
	/***/ public String couldNotRewindToUpstreamCommit;
	/***/ public String couldNotURLEncodeToUTF8;
	/***/ public String couldNotWriteFile;
	/***/ public String countingObjects;
	/***/ public String createBranchFailedUnknownReason;
	/***/ public String createBranchUnexpectedResult;
	/***/ public String createNewFileFailed;
	/***/ public String credentialPassword;
	/***/ public String credentialUsername;
	/***/ public String daemonAlreadyRunning;
	/***/ public String daysAgo;
	/***/ public String deleteBranchUnexpectedResult;
	/***/ public String deleteFileFailed;
	/***/ public String deleteTagUnexpectedResult;
	/***/ public String deletingNotSupported;
	/***/ public String destinationIsNotAWildcard;
	/***/ public String detachedHeadDetected;
	/***/ public String dirCacheDoesNotHaveABackingFile;
	/***/ public String dirCacheFileIsNotLocked;
	/***/ public String dirCacheIsNotLocked;
	/***/ public String DIRCChecksumMismatch;
	/***/ public String DIRCExtensionIsTooLargeAt;
	/***/ public String DIRCExtensionNotSupportedByThisVersion;
	/***/ public String DIRCHasTooManyEntries;
	/***/ public String DIRCUnrecognizedExtendedFlags;
	/***/ public String dirtyFilesExist;
	/***/ public String doesNotHandleMode;
	/***/ public String downloadCancelled;
	/***/ public String downloadCancelledDuringIndexing;
	/***/ public String duplicateAdvertisementsOf;
	/***/ public String duplicateRef;
	/***/ public String duplicateRemoteRefUpdateIsIllegal;
	/***/ public String duplicateStagesNotAllowed;
	/***/ public String eitherGitDirOrWorkTreeRequired;
	/***/ public String emptyCommit;
	/***/ public String emptyPathNotPermitted;
	/***/ public String emptyRef;
	/***/ public String encryptionError;
	/***/ public String encryptionOnlyPBE;
	/***/ public String endOfFileInEscape;
	/***/ public String entryNotFoundByPath;
	/***/ public String enumValueNotSupported2;
	/***/ public String enumValueNotSupported3;
	/***/ public String enumValuesNotAvailable;
	/***/ public String errorDecodingFromFile;
	/***/ public String errorEncodingFromFile;
	/***/ public String errorInBase64CodeReadingStream;
	/***/ public String errorInPackedRefs;
	/***/ public String errorInvalidProtocolWantedOldNewRef;
	/***/ public String errorListing;
	/***/ public String errorOccurredDuringUnpackingOnTheRemoteEnd;
	/***/ public String errorReadingInfoRefs;
	/***/ public String exceptionCaughtDuringExecutionOfHook;
	/***/ public String exceptionCaughtDuringExecutionOfAddCommand;
	/***/ public String exceptionCaughtDuringExecutionOfArchiveCommand;
	/***/ public String exceptionCaughtDuringExecutionOfCherryPickCommand;
	/***/ public String exceptionCaughtDuringExecutionOfCommitCommand;
	/***/ public String exceptionCaughtDuringExecutionOfFetchCommand;
	/***/ public String exceptionCaughtDuringExecutionOfLsRemoteCommand;
	/***/ public String exceptionCaughtDuringExecutionOfMergeCommand;
	/***/ public String exceptionCaughtDuringExecutionOfPullCommand;
	/***/ public String exceptionCaughtDuringExecutionOfPushCommand;
	/***/ public String exceptionCaughtDuringExecutionOfResetCommand;
	/***/ public String exceptionCaughtDuringExecutionOfRevertCommand;
	/***/ public String exceptionCaughtDuringExecutionOfRmCommand;
	/***/ public String exceptionCaughtDuringExecutionOfTagCommand;
	/***/ public String exceptionCaughtDuringExcecutionOfCommand;
	/***/ public String exceptionHookExecutionInterrupted;
	/***/ public String exceptionOccurredDuringAddingOfOptionToALogCommand;
	/***/ public String exceptionOccurredDuringReadingOfGIT_DIR;
	/***/ public String exceptionWhileReadingPack;
	/***/ public String expectedACKNAKFoundEOF;
	/***/ public String expectedACKNAKGot;
	/***/ public String expectedBooleanStringValue;
	/***/ public String expectedCharacterEncodingGuesses;
	/***/ public String expectedEOFReceived;
	/***/ public String expectedGot;
	/***/ public String expectedLessThanGot;
	/***/ public String expectedPktLineWithService;
	/***/ public String expectedReceivedContentType;
	/***/ public String expectedReportForRefNotReceived;
	/***/ public String failedToDetermineFilterDefinition;
	/***/ public String failedUpdatingRefs;
	/***/ public String failureDueToOneOfTheFollowing;
	/***/ public String failureUpdatingFETCH_HEAD;
	/***/ public String failureUpdatingTrackingRef;
	/***/ public String fileCannotBeDeleted;
	/***/ public String fileIsTooBigForThisConvenienceMethod;
	/***/ public String fileIsTooLarge;
	/***/ public String fileModeNotSetForPath;
	/***/ public String filterExecutionFailed;
	/***/ public String filterExecutionFailedRc;
	/***/ public String findingGarbage;
	/***/ public String flagIsDisposed;
	/***/ public String flagNotFromThis;
	/***/ public String flagsAlreadyCreated;
	/***/ public String funnyRefname;
	/***/ public String gcFailed;
	/***/ public String gitmodulesNotFound;
	/***/ public String headRequiredToStash;
	/***/ public String hoursAgo;
	/***/ public String hugeIndexesAreNotSupportedByJgitYet;
	/***/ public String hunkBelongsToAnotherFile;
	/***/ public String hunkDisconnectedFromFile;
	/***/ public String hunkHeaderDoesNotMatchBodyLineCountOf;
	/***/ public String illegalArgumentNotA;
	/***/ public String illegalCombinationOfArguments;
	/***/ public String illegalHookName;
	/***/ public String illegalPackingPhase;
	/***/ public String illegalStateExists;
	/***/ public String improperlyPaddedBase64Input;
	/***/ public String incorrectHashFor;
	/***/ public String incorrectOBJECT_ID_LENGTH;
	/***/ public String indexFileCorruptedNegativeBucketCount;
	/***/ public String indexFileIsInUse;
	/***/ public String indexFileIsTooLargeForJgit;
	/***/ public String indexSignatureIsInvalid;
	/***/ public String indexWriteException;
	/***/ public String initFailedBareRepoDifferentDirs;
	/***/ public String initFailedDirIsNoDirectory;
	/***/ public String initFailedGitDirIsNoDirectory;
	/***/ public String initFailedNonBareRepoSameDirs;
	/***/ public String inMemoryBufferLimitExceeded;
	/***/ public String inputDidntMatchLength;
	/***/ public String inputStreamMustSupportMark;
	/***/ public String integerValueOutOfRange;
	/***/ public String internalRevisionError;
	/***/ public String internalServerError;
	/***/ public String interruptedWriting;
	/***/ public String inTheFuture;
	/***/ public String invalidAdvertisementOf;
	/***/ public String invalidAncestryLength;
	/***/ public String invalidBooleanValue;
	/***/ public String invalidChannel;
	/***/ public String invalidCharacterInBase64Data;
	/***/ public String invalidCommitParentNumber;
	/***/ public String invalidDepth;
	/***/ public String invalidEncryption;
	/***/ public String invalidExpandWildcard;
	/***/ public String invalidGitdirRef;
	/***/ public String invalidGitType;
	/***/ public String invalidId;
	/***/ public String invalidId0;
	/***/ public String invalidIdLength;
	/***/ public String invalidIgnoreParamSubmodule;
	/***/ public String invalidIgnoreRule;
	/***/ public String invalidIntegerValue;
	/***/ public String invalidKey;
	/***/ public String invalidLineInConfigFile;
	/***/ public String invalidModeFor;
	/***/ public String invalidModeForPath;
	/***/ public String invalidObject;
	/***/ public String invalidOldIdSent;
	/***/ public String invalidPacketLineHeader;
	/***/ public String invalidPath;
	/***/ public String invalidPathContainsSeparator;
	/***/ public String invalidPathPeriodAtEndWindows;
	/***/ public String invalidPathSpaceAtEndWindows;
	/***/ public String invalidPathReservedOnWindows;
	/***/ public String invalidReflogRevision;
	/***/ public String invalidRefName;
	/***/ public String invalidRemote;
	/***/ public String invalidShallowObject;
	/***/ public String invalidStageForPath;
	/***/ public String invalidTagOption;
	/***/ public String invalidTimeout;
	/***/ public String invalidTimeUnitValue2;
	/***/ public String invalidTimeUnitValue3;
	/***/ public String invalidTreeZeroLengthName;
	/***/ public String invalidURL;
	/***/ public String invalidWildcards;
	/***/ public String invalidRefSpec;
	/***/ public String invalidRepositoryStateNoHead;
	/***/ public String invalidWindowSize;
	/***/ public String isAStaticFlagAndHasNorevWalkInstance;
	/***/ public String JRELacksMD5Implementation;
	/***/ public String kNotInRange;
	/***/ public String largeObjectExceedsByteArray;
	/***/ public String largeObjectExceedsLimit;
	/***/ public String largeObjectException;
	/***/ public String largeObjectOutOfMemory;
	/***/ public String lengthExceedsMaximumArraySize;
	/***/ public String listingAlternates;
	/***/ public String listingPacks;
	/***/ public String localObjectsIncomplete;
	/***/ public String localRefIsMissingObjects;
	/***/ public String localRepository;
	/***/ public String lockCountMustBeGreaterOrEqual1;
	/***/ public String lockError;
	/***/ public String lockOnNotClosed;
	/***/ public String lockOnNotHeld;
	/***/ public String malformedpersonIdentString;
	/***/ public String maxCountMustBeNonNegative;
	/***/ public String mergeConflictOnNonNoteEntries;
	/***/ public String mergeConflictOnNotes;
	/***/ public String mergeStrategyAlreadyExistsAsDefault;
	/***/ public String mergeStrategyDoesNotSupportHeads;
	/***/ public String mergeUsingStrategyResultedInDescription;
	/***/ public String mergeRecursiveConflictsWhenMergingCommonAncestors;
	/***/ public String mergeRecursiveReturnedNoCommit;
	/***/ public String mergeRecursiveTooManyMergeBasesFor;
	/***/ public String messageAndTaggerNotAllowedInUnannotatedTags;
	/***/ public String minutesAgo;
	/***/ public String missingAccesskey;
	/***/ public String missingConfigurationForKey;
	/***/ public String missingDeltaBase;
	/***/ public String missingForwardImageInGITBinaryPatch;
	/***/ public String missingObject;
	/***/ public String missingPrerequisiteCommits;
	/***/ public String missingRequiredParameter;
	/***/ public String missingSecretkey;
	/***/ public String mixedStagesNotAllowed;
	/***/ public String mkDirFailed;
	/***/ public String mkDirsFailed;
	/***/ public String month;
	/***/ public String months;
	/***/ public String monthsAgo;
	/***/ public String multipleMergeBasesFor;
	/***/ public String need2Arguments;
	/***/ public String needPackOut;
	/***/ public String needsAtLeastOneEntry;
	/***/ public String needsWorkdir;
	/***/ public String newlineInQuotesNotAllowed;
	/***/ public String noApplyInDelete;
	/***/ public String noClosingBracket;
	/***/ public String noCredentialsProvider;
	/***/ public String noHEADExistsAndNoExplicitStartingRevisionWasSpecified;
	/***/ public String noHMACsupport;
	/***/ public String noMergeBase;
	/***/ public String noMergeHeadSpecified;
	/***/ public String noSuchRef;
	/***/ public String notABoolean;
	/***/ public String notABundle;
	/***/ public String notADIRCFile;
	/***/ public String notAGitDirectory;
	/***/ public String notAPACKFile;
	/***/ public String notARef;
	/***/ public String notASCIIString;
	/***/ public String notAuthorized;
	/***/ public String notAValidPack;
	/***/ public String notFound;
	/***/ public String nothingToFetch;
	/***/ public String nothingToPush;
	/***/ public String notMergedExceptionMessage;
	/***/ public String noXMLParserAvailable;
	/***/ public String objectAtHasBadZlibStream;
	/***/ public String objectAtPathDoesNotHaveId;
	/***/ public String objectIsCorrupt;
	/***/ public String objectIsCorrupt3;
	/***/ public String objectIsNotA;
	/***/ public String objectNotFound;
	/***/ public String objectNotFoundIn;
	/***/ public String obtainingCommitsForCherryPick;
	/***/ public String offsetWrittenDeltaBaseForObjectNotFoundInAPack;
	/***/ public String onlyAlreadyUpToDateAndFastForwardMergesAreAvailable;
	/***/ public String onlyOneFetchSupported;
	/***/ public String onlyOneOperationCallPerConnectionIsSupported;
	/***/ public String openFilesMustBeAtLeast1;
	/***/ public String openingConnection;
	/***/ public String operationCanceled;
	/***/ public String outputHasAlreadyBeenStarted;
	/***/ public String packChecksumMismatch;
	/***/ public String packCorruptedWhileWritingToFilesystem;
	/***/ public String packDoesNotMatchIndex;
	/***/ public String packedRefsHandleIsStale;
	/***/ public String packetSizeMustBeAtLeast;
	/***/ public String packetSizeMustBeAtMost;
	/***/ public String packedRefsCorruptionDetected;
	/***/ public String packfileCorruptionDetected;
	/***/ public String packFileInvalid;
	/***/ public String packfileIsTruncated;
	/***/ public String packfileIsTruncatedNoParam;
	/***/ public String packHandleIsStale;
	/***/ public String packHasUnresolvedDeltas;
	/***/ public String packInaccessible;
	/***/ public String packingCancelledDuringObjectsWriting;
	/***/ public String packObjectCountMismatch;
	/***/ public String packRefs;
	/***/ public String packSizeNotSetYet;
	/***/ public String packTooLargeForIndexVersion1;
	/***/ public String packWasDeleted;
	/***/ public String packWriterStatistics;
	/***/ public String panicCantRenameIndexFile;
	/***/ public String patchApplyException;
	/***/ public String patchFormatException;
	/***/ public String pathIsNotInWorkingDir;
	/***/ public String pathNotConfigured;
	/***/ public String peeledLineBeforeRef;
	/***/ public String peerDidNotSupplyACompleteObjectGraph;
	/***/ public String personIdentEmailNonNull;
	/***/ public String personIdentNameNonNull;
	/***/ public String prefixRemote;
	/***/ public String problemWithResolvingPushRefSpecsLocally;
	/***/ public String progressMonUploading;
	/***/ public String propertyIsAlreadyNonNull;
	/***/ public String pruneLoosePackedObjects;
	/***/ public String pruneLooseUnreferencedObjects;
	/***/ public String pullOnRepoWithoutHEADCurrentlyNotSupported;
	/***/ public String pullTaskName;
	/***/ public String pushCancelled;
	/***/ public String pushCertificateInvalidField;
	/***/ public String pushCertificateInvalidFieldValue;
	/***/ public String pushCertificateInvalidHeader;
	/***/ public String pushCertificateInvalidSignature;
	/***/ public String pushIsNotSupportedForBundleTransport;
	/***/ public String pushNotPermitted;
	/***/ public String pushOptionsNotSupported;
	/***/ public String rawLogMessageDoesNotParseAsLogEntry;
	/***/ public String readerIsRequired;
	/***/ public String readingObjectsFromLocalRepositoryFailed;
	/***/ public String readTimedOut;
	/***/ public String receivePackObjectTooLarge1;
	/***/ public String receivePackObjectTooLarge2;
	/***/ public String receivePackInvalidLimit;
	/***/ public String receivePackTooLarge;
	/***/ public String receivingObjects;
	/***/ public String refAlreadyExists;
	/***/ public String refAlreadyExists1;
	/***/ public String reflogEntryNotFound;
	/***/ public String refNotResolved;
	/***/ public String refUpdateReturnCodeWas;
	/***/ public String remoteConfigHasNoURIAssociated;
	/***/ public String remoteDoesNotHaveSpec;
	/***/ public String remoteDoesNotSupportSmartHTTPPush;
	/***/ public String remoteHungUpUnexpectedly;
	/***/ public String remoteNameCantBeNull;
	/***/ public String renameBranchFailedBecauseTag;
	/***/ public String renameBranchFailedUnknownReason;
	/***/ public String renameBranchUnexpectedResult;
	/***/ public String renameFileFailed;
	/***/ public String renamesAlreadyFound;
	/***/ public String renamesBreakingModifies;
	/***/ public String renamesFindingByContent;
	/***/ public String renamesFindingExact;
	/***/ public String renamesRejoiningModifies;
	/***/ public String repositoryAlreadyExists;
	/***/ public String repositoryConfigFileInvalid;
	/***/ public String repositoryNotFound;
	/***/ public String repositoryState_applyMailbox;
	/***/ public String repositoryState_bare;
	/***/ public String repositoryState_bisecting;
	/***/ public String repositoryState_conflicts;
	/***/ public String repositoryState_merged;
	/***/ public String repositoryState_normal;
	/***/ public String repositoryState_rebase;
	/***/ public String repositoryState_rebaseInteractive;
	/***/ public String repositoryState_rebaseOrApplyMailbox;
	/***/ public String repositoryState_rebaseWithMerge;
	/***/ public String requiredHashFunctionNotAvailable;
	/***/ public String resettingHead;
	/***/ public String resolvingDeltas;
	/***/ public String resultLengthIncorrect;
	/***/ public String rewinding;
	/***/ public String s3ActionDeletion;
	/***/ public String s3ActionReading;
	/***/ public String s3ActionWriting;
	/***/ public String searchForReuse;
	/***/ public String searchForSizes;
	/***/ public String secondsAgo;
	/***/ public String selectingCommits;
	/***/ public String sequenceTooLargeForDiffAlgorithm;
	/***/ public String serviceNotEnabledNoName;
	/***/ public String serviceNotPermitted;
	/***/ public String shallowCommitsAlreadyInitialized;
	/***/ public String shallowPacksRequireDepthWalk;
	/***/ public String shortCompressedStreamAt;
	/***/ public String shortReadOfBlock;
	/***/ public String shortReadOfOptionalDIRCExtensionExpectedAnotherBytes;
	/***/ public String shortSkipOfBlock;
	/***/ public String signingNotSupportedOnTag;
	/***/ public String similarityScoreMustBeWithinBounds;
	/***/ public String sizeExceeds2GB;
	/***/ public String skipMustBeNonNegative;
	/***/ public String smartHTTPPushDisabled;
	/***/ public String sourceDestinationMustMatch;
	/***/ public String sourceIsNotAWildcard;
	/***/ public String sourceRefDoesntResolveToAnyObject;
	/***/ public String sourceRefNotSpecifiedForRefspec;
	/***/ public String squashCommitNotUpdatingHEAD;
	/***/ public String staleRevFlagsOn;
	/***/ public String startingReadStageWithoutWrittenRequestDataPendingIsNotSupported;
	/***/ public String stashApplyConflict;
	/***/ public String stashApplyConflictInIndex;
	/***/ public String stashApplyFailed;
	/***/ public String stashApplyWithoutHead;
	/***/ public String stashApplyOnUnsafeRepository;
	/***/ public String stashCommitIncorrectNumberOfParents;
	/***/ public String stashDropDeleteRefFailed;
	/***/ public String stashDropFailed;
	/***/ public String stashDropMissingReflog;
	/***/ public String stashFailed;
	/***/ public String stashResolveFailed;
	/***/ public String statelessRPCRequiresOptionToBeEnabled;
	/***/ public String storePushCertMultipleRefs;
	/***/ public String storePushCertOneRef;
	/***/ public String storePushCertReflog;
	/***/ public String submoduleExists;
	/***/ public String submodulesNotSupported;
	/***/ public String submoduleParentRemoteUrlInvalid;
	/***/ public String supportOnlyPackIndexVersion2;
	/***/ public String symlinkCannotBeWrittenAsTheLinkTarget;
	/***/ public String systemConfigFileInvalid;
	/***/ public String tagAlreadyExists;
	/***/ public String tagNameInvalid;
	/***/ public String tagOnRepoWithoutHEADCurrentlyNotSupported;
	/***/ public String transactionAborted;
	/***/ public String theFactoryMustNotBeNull;
	/***/ public String timeIsUncertain;
	/***/ public String timerAlreadyTerminated;
	/***/ public String tooManyIncludeRecursions;
	/***/ public String topologicalSortRequired;
	/***/ public String transportExceptionBadRef;
	/***/ public String transportExceptionEmptyRef;
	/***/ public String transportExceptionInvalid;
	/***/ public String transportExceptionMissingAssumed;
	/***/ public String transportExceptionReadRef;
	/***/ public String transportNeedsRepository;
	/***/ public String transportProtoAmazonS3;
	/***/ public String transportProtoBundleFile;
	/***/ public String transportProtoFTP;
	/***/ public String transportProtoGitAnon;
	/***/ public String transportProtoHTTP;
	/***/ public String transportProtoLocal;
	/***/ public String transportProtoSFTP;
	/***/ public String transportProtoSSH;
	/***/ public String transportProtoTest;
	/***/ public String transportProvidedRefWithNoObjectId;
	/***/ public String transportSSHRetryInterrupt;
	/***/ public String treeEntryAlreadyExists;
	/***/ public String treeFilterMarkerTooManyFilters;
	/***/ public String treeIteratorDoesNotSupportRemove;
	/***/ public String treeWalkMustHaveExactlyTwoTrees;
	/***/ public String truncatedHunkLinesMissingForAncestor;
	/***/ public String truncatedHunkNewLinesMissing;
	/***/ public String truncatedHunkOldLinesMissing;
	/***/ public String tSizeMustBeGreaterOrEqual1;
	/***/ public String unableToCheckConnectivity;
	/***/ public String unableToCreateNewObject;
	/***/ public String unableToStore;
	/***/ public String unableToWrite;
	/***/ public String unauthorized;
	/***/ public String unencodeableFile;
	/***/ public String unexpectedCompareResult;
	/***/ public String unexpectedEndOfConfigFile;
	/***/ public String unexpectedEndOfInput;
	/***/ public String unexpectedHunkTrailer;
	/***/ public String unexpectedOddResult;
	/***/ public String unexpectedRefReport;
	/***/ public String unexpectedReportLine;
	/***/ public String unexpectedReportLine2;
	/***/ public String unknownOrUnsupportedCommand;
	/***/ public String unknownDIRCVersion;
	/***/ public String unknownHost;
	/***/ public String unknownIndexVersionOrCorruptIndex;
	/***/ public String unknownObject;
	/***/ public String unknownObjectType;
	/***/ public String unknownObjectType2;
	/***/ public String unknownRepositoryFormat;
	/***/ public String unknownRepositoryFormat2;
	/***/ public String unknownZlibError;
	/***/ public String unmergedPath;
	/***/ public String unmergedPaths;
	/***/ public String unpackException;
	/***/ public String unreadablePackIndex;
	/***/ public String unrecognizedRef;
	/***/ public String unsetMark;
	/***/ public String unsupportedAlternates;
	/***/ public String unsupportedArchiveFormat;
	/***/ public String unsupportedCommand0;
	/***/ public String unsupportedEncryptionAlgorithm;
	/***/ public String unsupportedEncryptionVersion;
	/***/ public String unsupportedGC;
	/***/ public String unsupportedMark;
	/***/ public String unsupportedOperationNotAddAtEnd;
	/***/ public String unsupportedPackIndexVersion;
	/***/ public String unsupportedPackVersion;
	/***/ public String unsupportedRepositoryDescription;
	/***/ public String updatingHeadFailed;
	/***/ public String updatingReferences;
	/***/ public String updatingRefFailed;
	/***/ public String upstreamBranchName;
	/***/ public String uriNotConfigured;
	/***/ public String uriNotFound;
	/***/ public String URINotSupported;
	/***/ public String URLNotFound;
	/***/ public String userConfigFileInvalid;
	/***/ public String walkFailure;
	/***/ public String wantNotValid;
	/***/ public String weeksAgo;
	/***/ public String windowSizeMustBeLesserThanLimit;
	/***/ public String windowSizeMustBePowerOf2;
	/***/ public String writerAlreadyInitialized;
	/***/ public String writeTimedOut;
	/***/ public String writingNotPermitted;
	/***/ public String writingNotSupported;
	/***/ public String writingObjects;
	/***/ public String wrongDecompressedLength;
	/***/ public String wrongRepositoryState;
	/***/ public String year;
	/***/ public String years;
	/***/ public String years0MonthsAgo;
	/***/ public String yearsAgo;
	/***/ public String yearsMonthsAgo;
}
