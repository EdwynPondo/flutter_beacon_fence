import 'package:flutter/services.dart';
import 'package:flutter_beacon_fence/flutter_beacon_fence.dart';
import 'package:flutter_beacon_fence/src/generated/platform_bindings.g.dart';
import 'package:flutter_beacon_fence/src/model/model_mapper.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('BeaconFenceExceptionMapper.fromPlatformException', () {
    for (final code in BeaconFenceErrorCode.values) {
      test('maps native code "${code.index}" to ${code.name}', () {
        final ex = BeaconFenceExceptionMapper.fromPlatformException(
          PlatformException(code: code.index.toString()),
        );
        expect(ex.code, code);
      });
    }

    test('maps Pigeon channel-error to channelError', () {
      final ex = BeaconFenceExceptionMapper.fromPlatformException(
        PlatformException(code: 'channel-error'),
      );
      expect(ex.code, BeaconFenceErrorCode.channelError);
    });

    test('maps unexpected native exception class names to unknown', () {
      for (final code in ['IllegalStateException', 'NSError', '']) {
        final ex = BeaconFenceExceptionMapper.fromPlatformException(
          PlatformException(code: code),
        );
        expect(ex.code, BeaconFenceErrorCode.unknown, reason: code);
      }
    });

    test('maps out of range index to unknown', () {
      final ex = BeaconFenceExceptionMapper.fromPlatformException(
        PlatformException(code: '999'),
      );
      expect(ex.code, BeaconFenceErrorCode.unknown);
    });

    test('copies message, details and stacktrace', () {
      final ex = BeaconFenceExceptionMapper.fromPlatformException(
        PlatformException(
          code: BeaconFenceErrorCode.beaconNotFound.index.toString(),
          message: 'Beacon not found',
          details: {'id': 'abc'},
          stacktrace: 'native stack',
        ),
      );
      expect(ex.message, 'Beacon not found');
      expect(ex.details, {'id': 'abc'});
      expect(ex.stacktrace, 'native stack');
    });
  });

  group('BeaconFenceExceptionMapper.fromError', () {
    test('returns BeaconFenceException unchanged', () {
      final original = BeaconFenceException.invalidArgument(message: 'bad');
      expect(BeaconFenceExceptionMapper.fromError(original), same(original));
    });

    test('maps PlatformException through its code', () {
      final ex = BeaconFenceExceptionMapper.fromError(
        PlatformException(
          code: BeaconFenceErrorCode.invalidArguments.index.toString(),
        ),
      );
      expect(ex.code, BeaconFenceErrorCode.invalidArguments);
    });

    test('maps generic Exception to unknown', () {
      final ex = BeaconFenceExceptionMapper.fromError(Exception('boom'));
      expect(ex.code, BeaconFenceErrorCode.unknown);
      expect(ex.message, contains('boom'));
    });

    test('maps non-Exception error to unknown', () {
      final ex = BeaconFenceExceptionMapper.fromError('boom');
      expect(ex.code, BeaconFenceErrorCode.unknown);
      expect(ex.message, 'boom');
    });
  });
}
