extension BeaconFenceErrorCode {
    /// Builds a `PigeonError` whose code is decoded on the Dart side into a
    /// `BeaconFenceErrorCode`. Use only for business logic or validation
    /// failures; unexpected errors should be forwarded as-is and surface as
    /// `unknown`.
    func toPigeonError(_ message: String?) -> PigeonError {
        PigeonError(code: String(rawValue), message: message, details: nil)
    }
}
