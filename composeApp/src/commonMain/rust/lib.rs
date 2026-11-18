mod metadata;
mod discord_rpc;
mod spotify_connect_receiver;

pub use metadata::*;
pub use discord_rpc::*;
pub use spotify_connect_receiver::*;

uniffi::setup_scaffolding!();