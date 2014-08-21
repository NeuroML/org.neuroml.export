--
-- Parameterisable N to M mux.
--

library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- For Modelsim
--use ieee.fixed_pkg.all;
--use ieee.fixed_float_types.ALL;

-- For ISE
library ieee_proposed;
use ieee_proposed.fixed_pkg.all;
use ieee_proposed.fixed_float_types.ALL;
use IEEE.numeric_std.all;

entity ParamExp is
	port(
		clk		: In  Std_logic;
		rst		: In  Std_logic;
		Start	: In  Std_logic;
		Done	: Out  Std_logic;
		X		: In sfixed(11 downto -12);
		Output	: Out sfixed(11 downto -12)
		);
end ParamExp;

architecture RTL of ParamExp is
type MEM is array (0 to 7) of sfixed(11 downto -12);
signal output_internal : sfixed(11 downto -12);
signal currentTerm : sfixed(11 downto -12);
signal COUNT : unsigned(3 downto 0) := "1001";
begin
	
	process(clk)
		variable MEM8Xsfixed : MEM := (to_sfixed (1,11, -12),to_sfixed (0.5,11, -12),to_sfixed (0.33333333,11, -12),to_sfixed (0.25,11, -12),
		to_sfixed (0.2,11, -12),to_sfixed (0.16666666667,11, -12),to_sfixed (0.142857142857,11, -12),to_sfixed (0.125,11, -12));
		variable Sel : integer;
		begin 
			if rst = '1' then
				COUNT <= "1001";
				currentTerm <= to_sfixed (0,11, -12);
				output_internal <= to_sfixed (0,11, -12);
			elsif clk'event and clk = '1' then
				if Start = '1' then
					Done <= '0';
					COUNT <= "0000"; 	
					currentTerm <= to_sfixed (1,11, -12);
					output_internal <= to_sfixed (0,11, -12);
				elsif COUNT /= "1001" then
					Done <= '0';
					output_internal <= resize(output_internal + currentTerm,11,-12);
					currentTerm <= resize(MEM8Xsfixed(to_integer(unsigned(COUNT(2 downto 0)))) * resize(X * currentTerm,11,-12),11,-12);
					COUNT <= COUNT + 1;
				elsif COUNT = "1001" then 
					Done <= '1';
				end if; 
			end if;
	end process;
	Output <= output_internal;
end RTL;
		
