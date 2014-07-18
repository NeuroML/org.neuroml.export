--
-- Parameterisable N to M mux.
--

LIBRARY ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;

entity ParamMux is
	generic( 
		NSpikeSources 	: integer := 32;	-- The number of spike sources.
		NOutputs		: integer := 16;	-- The number of Synapses in the neuron model.
		NSelectBits		: integer := 5);	-- Log2(NSpikeSources), rounded up.
	port(
		SpikeIn			: In  Std_logic_vector((NSpikeSources-1) downto 0);
		SelectIn		: In  Std_logic_vector(((NOutputs*NSelectBits)-1) downto 0);
		SpikeOut		: Out Std_logic_vector((NOutputs-1) downto 0));
end ParamMux;

architecture RTL of ParamMux is

		
begin
	
	--
	-- Convert the incoming select signals to integer so we can use them as an index,
	-- then use them as an index into SpikeIn to make a mux.
	--
	process(SelectIn,SpikeIn)
		variable Sel : integer;
	begin
		for i in 0 to (NOutputs-1) loop
			Sel:= conv_integer(SelectIn((i*NSelectBits+NSelectBits-1) downto (i*NSelectBits)));
			SpikeOut(i) <= SpikeIn(Sel);
		end loop;
	end process;

end RTL;
		
