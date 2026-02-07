import 'package:flutter_beacon_fence/src/generated/platform_bindings.g.dart';

/// All exceptions thrown by flutter_beacon_fence will be of this type.
class BeaconFenceException implements Exception {
  final BeaconFenceErrorCode code;
  final String? message;
  final dynamic details;
  final String? stacktrace;

  BeaconFenceException({
    required this.code,
    this.message,
    this.details,
    this.stacktrace,
  });

  BeaconFenceException.internal({
    required String this.message,
    this.details,
  })  : code = BeaconFenceErrorCode.pluginInternal,
        stacktrace = StackTrace.current.toString();

  BeaconFenceException.invalidArgument({
    required String this.message,
    this.details,
  })  : code = BeaconFenceErrorCode.invalidArguments,
        stacktrace = StackTrace.current.toString();

  @override
  String toString() => 'BeaconFenceException(${code.name}, message=$message, '
      'details=$details, stacktrace=$stacktrace)';
}
