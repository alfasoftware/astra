# Changelog
All notable changes to this project will be documented in this file.

## [1.3.1] - 2021-06-04
### Added

* Added a new ASTOperation the JavaPatternASTOperation, [read more in the wiki](https://github.com/alfasoftware/astra/wiki/Java-Pattern-Refactor).

### Changed

* ClassVisitor visits additional ASTNodes

### Fixed

## [1.2.1] - 2021-05-19
### Added

### Changed

* Type matcher now matches on parameterized supertypes
* Method matcher now allows custom predicates to be used for matching any AST node types handled by MethodMatcher

### Fixed

* Fix unused imports refactor so it doesn't lose inner types.
* Log the total number of files after filtering with the predicate

## [1.0.0] - 2021-03-06
### Added

* Initial commit! Please see our [README](https://github.com/alfasoftware/astra/blob/main/README.md) and [Wiki](https://github.com/alfasoftware/astra/wiki) for details on how to get started.

### Changed

### Fixed
