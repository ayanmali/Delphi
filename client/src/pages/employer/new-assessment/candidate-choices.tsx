// import React, { useState } from 'react';
// import { ChevronDown, ChevronUp, Plus, X, Code, Users } from 'lucide-react';
// import { FormLabel } from '@/components/ui/form';
// import { Control, useController, FieldValues, Path, PathValue } from 'react-hook-form';

// interface CandidateChoicesData {
//     enableLanguageSelection: boolean;
//     languageOptions: string[];
// }

// interface ChoiceConfigProps<T extends FieldValues = FieldValues> {
//     control: Control<T>;
//     name: Path<T>;
// }

// const ChoiceConfig = <T extends FieldValues = FieldValues>({ control, name }: ChoiceConfigProps<T>) => {
//     const [isLanguageSectionOpen, setIsLanguageSectionOpen] = useState(false);
//     const [newLanguageName, setNewLanguageName] = useState('');
//     const [isAddingNew, setIsAddingNew] = useState(false);

//     const {
//         field: { value, onChange },
//         fieldState: { error }
//     } = useController({
//         name,
//         control,
//         defaultValue: {
//             enableLanguageSelection: false,
//             languageOptions: []
//         } as PathValue<T, Path<T>>
//     });

//     const addLanguageOption = () => {
//         if (newLanguageName.trim()) {
//             onChange({
//                 ...value,
//                 languageOptions: [...value.languageOptions, newLanguageName.trim()]
//             });
//             setNewLanguageName('');
//             setIsAddingNew(false);
//         }
//     };

//     const removeLanguageOption = (option: string) => {
//         onChange({
//             ...value,
//             languageOptions: value.languageOptions.filter((op: string) => op !== option)
//         });
//     };

//     const toggleLanguageSelection = (enabled: boolean) => {
//         console.log("Value: ", value);
//         onChange({
//             ...value,
//             enableLanguageSelection: enabled,
//             languageOptions: enabled ? value.languageOptions : []
//         })
//         return;
//     };

//     const handleKeyPress = (e: React.KeyboardEvent) => {
//         if (e.key === 'Enter') {
//             addLanguageOption();
//         } else if (e.key === 'Escape') {
//             setIsAddingNew(false);
//             setNewLanguageName('');
//         }
//     };

//     return (
//         <div className="space-y-8">
//             {/* Language/Framework Selection Section */}
//             <div className="border border-gray-700 rounded-lg bg-gray-750">
//                 <button
//                     type="button"
//                     onClick={() => setIsLanguageSectionOpen(!isLanguageSectionOpen)}
//                     className="w-full px-6 py-4 flex items-center justify-between text-left hover:bg-gray-700 transition-colors rounded-lg"
//                 >
//                     <div className="flex-none">
//                         <FormLabel className="text-slate-300 font-medium">Language and Framework Options</FormLabel>
//                         <p className="text-sm text-gray-400">Allow candidates to choose their preferred technology</p>
//                     </div>
//                     {isLanguageSectionOpen ? (
//                         <ChevronUp className="w-5 h-5 text-gray-400" />
//                     ) : (
//                         <ChevronDown className="w-5 h-5 text-gray-400" />
//                     )}
//                 </button>

//                 {isLanguageSectionOpen && (
//                     <div className="px-6 pb-6 space-y-6">
//                         <div className="border-t border-gray-700 pt-6">
//                             <div className="flex items-center gap-3 mb-6">
//                                 <input
//                                     type="checkbox"
//                                     id="enableLanguageSelection"
//                                     checked={value.enableLanguageSelection}
//                                     onChange={(e) => toggleLanguageSelection(e.target.checked)}
//                                     className="w-4 h-4 text-blue-600 bg-gray-700 border-gray-600 rounded focus:ring-blue-500 focus:ring-2"
//                                 />
//                                 <label htmlFor="enableLanguageSelection" className="text-white font-medium text-sm">
//                                     Enable language/framework selection for candidates
//                                 </label>
//                             </div>

//                             {value.enableLanguageSelection && (
//                                 <div className="space-y-4">
//                                     <div className="flex items-center justify-between">
//                                         <h4 className="text-white font-medium">Available Options</h4>
//                                         <button
//                                             type="button"
//                                             onClick={() => setIsAddingNew(true)}
//                                             className="flex items-center gap-2 px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors text-sm font-medium"
//                                         >
//                                             <Plus className="w-4 h-4" />
//                                             Add Option
//                                         </button>
//                                     </div>

//                                     <div className="grid gap-3">
//                                         {(value.languageOptions ?? []).map((option: string) => (
//                                             <div
//                                                 key={option}
//                                                 className="flex items-center justify-between p-4 bg-gray-700 rounded-lg border border-gray-600"
//                                             >
//                                                 <span className="text-white font-medium">{option}</span>
//                                                 <button
//                                                     type="button"
//                                                     onClick={() => removeLanguageOption(option)}
//                                                     className="p-1 text-gray-400 hover:text-red-400 hover:bg-red-900/20 rounded transition-colors"
//                                                 >
//                                                     <X className="w-4 h-4" />
//                                                 </button>
//                                             </div>
//                                         ))}

//                                         {isAddingNew && (
//                                             <div className="flex items-center gap-3 p-4 bg-gray-700 rounded-lg border border-blue-500">
//                                                 <input
//                                                     type="text"
//                                                     value={newLanguageName}
//                                                     onChange={(e) => setNewLanguageName(e.target.value)}
//                                                     onKeyDown={handleKeyPress}
//                                                     placeholder="Enter language/framework name"
//                                                     className="flex-1 px-3 py-2 bg-gray-600 border border-gray-500 rounded text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
//                                                     autoFocus
//                                                 />
//                                                 <div className="flex gap-2">
//                                                     <button
//                                                         type="button"
//                                                         onClick={addLanguageOption}
//                                                         className="px-3 py-2 bg-green-600 hover:bg-green-700 text-white rounded text-sm font-medium transition-colors"
//                                                     >
//                                                         Add
//                                                     </button>
//                                                     <button
//                                                         type="button"
//                                                         onClick={() => {
//                                                             setIsAddingNew(false);
//                                                             setNewLanguageName('');
//                                                         }}
//                                                         className="px-3 py-2 bg-gray-600 hover:bg-gray-500 text-white rounded text-sm font-medium transition-colors"
//                                                     >
//                                                         Cancel
//                                                     </button>
//                                                 </div>
//                                             </div>
//                                         )}
//                                     </div>

//                                     {value.languageOptions.length === 0 && !isAddingNew && (
//                                         <div className="text-center py-8 text-gray-400">
//                                             <Code className="w-12 h-12 mx-auto mb-3 opacity-50" />
//                                             <p className="text-sm">No language options configured</p>
//                                             <p className="text-xs">Click "Add Option" to get started</p>
//                                         </div>
//                                     )}

//                                     {value.languageOptions.length > 0 && (
//                                         <div className="mt-6 p-4 bg-blue-900/20 border border-blue-700 rounded-lg">
//                                             <div className="flex items-center gap-2 mb-2">
//                                                 <Users className="w-4 h-4 text-blue-400" />
//                                                 <span className="text-sm font-medium text-blue-300">Candidate Experience</span>
//                                             </div>
//                                             <p className="text-sm text-blue-200">
//                                                 Candidates will see these options at the start of their assessment and can choose their preferred technology to complete their tasks.
//                                             </p>
//                                         </div>
//                                     )}
//                                 </div>
//                             )}
//                         </div>
//                     </div>
//                 )}
//             </div>
//             {error && (
//                 <p className="text-red-400 text-sm mt-1">{error.message}</p>
//             )}
//         </div>
//     );
// };

// export default ChoiceConfig;